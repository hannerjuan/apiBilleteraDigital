package com.billpay.service;

import com.billpay.dto.request.DepositRequest;
import com.billpay.dto.request.WithdrawRequest;
import com.billpay.dto.response.AccountResponse;
import com.billpay.dto.response.TransactionResponse;
import com.billpay.exception.DailyLimitExceededException;
import com.billpay.exception.InsufficientFundsException;
import com.billpay.exception.InvalidOperationException;
import com.billpay.model.Cuenta;
import com.billpay.model.EstadoCuenta;
import com.billpay.model.EstadoTransaccion;
import com.billpay.model.TipoTransaccion;
import com.billpay.model.Transaccion;
import com.billpay.model.Usuario;
import com.billpay.repository.CuentaRepository;
import com.billpay.repository.TransaccionRepository;
import com.billpay.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private AccountService accountService;

    private Usuario usuario;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountService, "minDepositAmount", BigDecimal.ONE);
        ReflectionTestUtils.setField(accountService, "maxDepositAmount", BigDecimal.valueOf(10000.00));
        ReflectionTestUtils.setField(accountService, "minWithdrawAmount", BigDecimal.ONE);
        ReflectionTestUtils.setField(accountService, "maxWithdrawAmount", BigDecimal.valueOf(5000.00));
        ReflectionTestUtils.setField(accountService, "dailyWithdrawalLimit", BigDecimal.valueOf(10000.00));

        usuario = new Usuario("55555", "Test Holder", "holder@example.com", "secret");
        usuario.setId(1L);
        cuenta = new Cuenta(usuario, "1234567890", BigDecimal.valueOf(1000.00), EstadoCuenta.ACTIVA);
        cuenta.setId(5L);
        usuario.setCuenta(cuenta);
    }

    @Test
    @DisplayName("Debe consultar balance y datos de la cuenta exitosamente")
    void getAccountByUser_Success() {
        when(usuarioRepository.findByEmail("holder@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuario(usuario)).thenReturn(Optional.of(cuenta));

        AccountResponse response = accountService.getAccountByUser("holder@example.com");

        assertNotNull(response);
        assertEquals("1234567890", response.getNumeroCuenta());
        assertEquals(BigDecimal.valueOf(1000.00), response.getSaldo());
        assertEquals("Test Holder", response.getTitular());
    }

    @Test
    @DisplayName("Debe ejecutar un depósito y acreditar el balance")
    void deposit_Success() {
        DepositRequest request = new DepositRequest(BigDecimal.valueOf(500.00), "Depósito prueba");

        when(usuarioRepository.findByEmail("holder@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuario(usuario)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByIdWithLock(5L)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> {
            Transaccion t = invocation.getArgument(0);
            t.setId(1L);
            t.setFechaCreacion(LocalDateTime.now());
            return t;
        });

        TransactionResponse response = accountService.deposit("holder@example.com", request);

        assertNotNull(response);
        assertEquals(TipoTransaccion.DEPOSITO, response.getTipo());
        assertEquals(0, BigDecimal.valueOf(1500.00).compareTo(cuenta.getSaldo()));
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    @DisplayName("Debe rechazar un depósito que exceda el monto máximo permitido")
    void deposit_ExceedsMaxAmount() {
        DepositRequest request = new DepositRequest(BigDecimal.valueOf(25000.00), "Depósito gigante");

        assertThrows(InvalidOperationException.class, () -> accountService.deposit("holder@example.com", request));
    }

    @Test
    @DisplayName("Debe ejecutar un retiro debitando el saldo exitosamente")
    void withdraw_Success() {
        WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(200.00), "Retiro cajero");

        when(usuarioRepository.findByEmail("holder@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuario(usuario)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByIdWithLock(5L)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.sumAmountByCuentaOrigenAndTipoAndEstadoSince(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> {
            Transaccion t = invocation.getArgument(0);
            t.setId(2L);
            t.setFechaCreacion(LocalDateTime.now());
            return t;
        });

        TransactionResponse response = accountService.withdraw("holder@example.com", request);

        assertNotNull(response);
        assertEquals(TipoTransaccion.RETIRO, response.getTipo());
        assertEquals(0, BigDecimal.valueOf(800.00).compareTo(cuenta.getSaldo()));
        verify(cuentaRepository).save(cuenta);
    }

    @Test
    @DisplayName("Debe rechazar el retiro si supera el límite diario acumulado")
    void withdraw_ExceedsDailyLimit() {
        WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(2000.00), "Retiro");

        when(usuarioRepository.findByEmail("holder@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuario(usuario)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByIdWithLock(5L)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.sumAmountByCuentaOrigenAndTipoAndEstadoSince(any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(9000.00)); // Ya retiró 9000 hoy, límite es 10000

        assertThrows(DailyLimitExceededException.class, () -> accountService.withdraw("holder@example.com", request));
    }

    @Test
    @DisplayName("Debe rechazar el retiro si los fondos son insuficientes")
    void withdraw_InsufficientFunds() {
        WithdrawRequest request = new WithdrawRequest(BigDecimal.valueOf(3000.00), "Retiro");

        when(usuarioRepository.findByEmail("holder@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByUsuario(usuario)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findByIdWithLock(5L)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.sumAmountByCuentaOrigenAndTipoAndEstadoSince(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        assertThrows(InsufficientFundsException.class, () -> accountService.withdraw("holder@example.com", request));
    }
}
