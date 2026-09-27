package com.billpay.integration;

import com.billpay.dto.request.RegisterRequest;
import com.billpay.dto.request.TransferRequest;
import com.billpay.dto.response.AuthResponse;
import com.billpay.model.Cuenta;
import com.billpay.repository.CuentaRepository;
import com.billpay.service.AuthService;
import com.billpay.service.TransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyDoubleSpendingIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Test
    @DisplayName("Concurrencia y ACID: Previene Double Spending bajo ráfagas de transferencias concurrentes con PESSIMISTIC_WRITE")
    void testConcurrencyAndDoubleSpendingPrevention() throws InterruptedException {
        // Cuenta A con saldo de $1,000.00
        AuthResponse userA = authService.register(new RegisterRequest(
                "DOC_CONCURRENCY_A",
                "Emisor Concurrente",
                "emisor.concurrente@test.com",
                "Password123!",
                BigDecimal.valueOf(1000.00)
        ));

        // Cuenta B con saldo inicial de $0.00
        AuthResponse userB = authService.register(new RegisterRequest(
                "DOC_CONCURRENCY_B",
                "Receptor Concurrente",
                "receptor.concurrente@test.com",
                "Password123!",
                BigDecimal.ZERO
        ));

        int numberOfThreads = 10;
        BigDecimal transferAmountPerThread = BigDecimal.valueOf(200.00);
        // Monto total intentado: 10 * 200 = 2,000.00 (el saldo es solo 1,000.00)

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successfulTransfers = new AtomicInteger(0);
        AtomicInteger failedTransfers = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    // Esperar a que todos los hilos arranquen al mismo tiempo
                    startLatch.await();
                    transferService.transfer(
                            "emisor.concurrente@test.com",
                            new TransferRequest(userB.getNumeroCuenta(), transferAmountPerThread, "Test Concurrencia")
                    );
                    successfulTransfers.incrementAndGet();
                } catch (Exception ex) {
                    failedTransfers.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Disparar simultáneamente los 10 hilos
        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "La prueba de concurrencia excedió el tiempo límite.");
        executorService.shutdown();

        // Verificación estricta de invariantes bancarias y ACID
        Cuenta cuentaAFinal = cuentaRepository.findByNumeroCuenta(userA.getNumeroCuenta()).orElseThrow();
        Cuenta cuentaBFinal = cuentaRepository.findByNumeroCuenta(userB.getNumeroCuenta()).orElseThrow();

        // Exactamente 5 transferencias deben ser exitosas ($200 * 5 = $1,000.00)
        assertEquals(5, successfulTransfers.get(), "Exactamente 5 transferencias debieron completarse.");
        assertEquals(5, failedTransfers.get(), "Exactamente 5 transferencias debieron ser rechazadas por fondos insuficientes.");

        // Saldo final de A debe ser exactamente $0.00 y NUNCA negativo
        assertEquals(0, BigDecimal.ZERO.compareTo(cuentaAFinal.getSaldo()), "El saldo de la cuenta A debe ser exactamente 0.00");
        assertTrue(cuentaAFinal.getSaldo().compareTo(BigDecimal.ZERO) >= 0, "El saldo nunca debe ser menor a cero.");

        // Saldo final de B debe ser exactamente $1,000.00
        assertEquals(0, BigDecimal.valueOf(1000.00).compareTo(cuentaBFinal.getSaldo()), "El saldo de la cuenta B debe ser exactamente 1,000.00");

        // Conservación del dinero: Saldo A + Saldo B = 1,000.00
        BigDecimal saldoTotal = cuentaAFinal.getSaldo().add(cuentaBFinal.getSaldo());
        assertEquals(0, BigDecimal.valueOf(1000.00).compareTo(saldoTotal), "Conservación total del dinero en el sistema.");
    }
}
