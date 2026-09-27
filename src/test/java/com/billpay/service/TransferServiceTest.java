package com.billpay.service;

import com.billpay.dto.request.TransferRequest;
import com.billpay.dto.response.TransactionResponse;
import com.billpay.exception.InactiveAccountException;
import com.billpay.exception.InsufficientFundsException;
import com.billpay.exception.InvalidOperationException;
import com.billpay.exception.ResourceNotFoundException;
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
class TransferServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private TransferService transferService;

    private Usuario userOrigen;
    private Cuenta cuentaOrigen;
    private Usuario userDestino;
    private Cuenta cuentaDestino;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transferService, "minTransferAmount", BigDecimal.ONE);

        userOrigen = new Usuario("1001", "Origen User", "origen@test.com", "pass");
        userOrigen.setId(1L);
        cuentaOrigen = new Cuenta(userOrigen, "1000000001", BigDecimal.valueOf(500.00), EstadoCuenta.ACTIVA);
        cuentaOrigen.setId(10L);
        userOrigen.setCuenta(cuentaOrigen);

        userDestino = new Usuario("1002", "Destino User", "destino@test.com", "pass");
        userDestino.setId(2L);
        cuentaDestino = new Cuenta(userDestino, "1000000002", BigDecimal.valueOf(100.00), EstadoCuenta.ACTIVA);
        cuentaDestino.setId(20L);
        userDestino.setCuenta(cuentaDestino);
    }

    @Test
    @DisplayName("Debe ejecutar transferencia atómica exitosamente deduciendo y acreditando simultáneamente")
    void transfer_Success() {
        TransferRequest request = new TransferRequest("1000000002", BigDecimal.valueOf(200.00), "Pago compartido");

        when(usuarioRepository.findByEmail("origen@test.com")).thenReturn(Optional.of(userOrigen));
        when(cuentaRepository.findByUsuario(userOrigen)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByNumeroCuenta("1000000002")).thenReturn(Optional.of(cuentaDestino));

        // Mock ordered pessimistic locking
        when(cuentaRepository.findByIdWithLock(10L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdWithLock(20L)).thenReturn(Optional.of(cuentaDestino));

        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> {
            Transaccion t = invocation.getArgument(0);
            t.setId(99L);
            t.setFechaCreacion(LocalDateTime.now());
            return t;
        });

        TransactionResponse response = transferService.transfer("origen@test.com", request);

        assertNotNull(response);
        assertEquals(99L, response.getId());
        assertEquals("1000000001", response.getCuentaOrigen());
        assertEquals("1000000002", response.getCuentaDestino());
        assertEquals(BigDecimal.valueOf(200.00), response.getMonto());
        assertEquals(TipoTransaccion.TRANSFERENCIA, response.getTipo());
        assertEquals(EstadoTransaccion.COMPLETADA, response.getEstado());

        // Verificar saldos actualizados
        assertEquals(0, BigDecimal.valueOf(300.00).compareTo(cuentaOrigen.getSaldo()));
        assertEquals(0, BigDecimal.valueOf(300.00).compareTo(cuentaDestino.getSaldo()));

        verify(cuentaRepository).save(cuentaOrigen);
        verify(cuentaRepository).save(cuentaDestino);
        verify(transaccionRepository).save(any(Transaccion.class));
    }

    @Test
    @DisplayName("Debe rechazar transferencia si la cuenta origen tiene fondos insuficientes")
    void transfer_InsufficientFunds() {
        TransferRequest request = new TransferRequest("1000000002", BigDecimal.valueOf(1000.00), "Monto excesivo");

        when(usuarioRepository.findByEmail("origen@test.com")).thenReturn(Optional.of(userOrigen));
        when(cuentaRepository.findByUsuario(userOrigen)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByNumeroCuenta("1000000002")).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.findByIdWithLock(10L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdWithLock(20L)).thenReturn(Optional.of(cuentaDestino));

        assertThrows(InsufficientFundsException.class, () -> transferService.transfer("origen@test.com", request));
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar transferencia si la cuenta origen no está activa")
    void transfer_OriginAccountInactive() {
        cuentaOrigen.setEstado(EstadoCuenta.BLOQUEADA);
        TransferRequest request = new TransferRequest("1000000002", BigDecimal.valueOf(50.00), "Test");

        when(usuarioRepository.findByEmail("origen@test.com")).thenReturn(Optional.of(userOrigen));
        when(cuentaRepository.findByUsuario(userOrigen)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByNumeroCuenta("1000000002")).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.findByIdWithLock(10L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdWithLock(20L)).thenReturn(Optional.of(cuentaDestino));

        assertThrows(InactiveAccountException.class, () -> transferService.transfer("origen@test.com", request));
    }

    @Test
    @DisplayName("Debe rechazar transferencia si la cuenta destino no está activa")
    void transfer_DestAccountInactive() {
        cuentaDestino.setEstado(EstadoCuenta.CANCELADA);
        TransferRequest request = new TransferRequest("1000000002", BigDecimal.valueOf(50.00), "Test");

        when(usuarioRepository.findByEmail("origen@test.com")).thenReturn(Optional.of(userOrigen));
        when(cuentaRepository.findByUsuario(userOrigen)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByNumeroCuenta("1000000002")).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.findByIdWithLock(10L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdWithLock(20L)).thenReturn(Optional.of(cuentaDestino));

        assertThrows(InactiveAccountException.class, () -> transferService.transfer("origen@test.com", request));
    }

    @Test
    @DisplayName("Debe rechazar si la cuenta origen y destino son la misma")
    void transfer_SameAccount() {
        TransferRequest request = new TransferRequest("1000000001", BigDecimal.valueOf(50.00), "Test");

        when(usuarioRepository.findByEmail("origen@test.com")).thenReturn(Optional.of(userOrigen));
        when(cuentaRepository.findByUsuario(userOrigen)).thenReturn(Optional.of(cuentaOrigen));

        assertThrows(InvalidOperationException.class, () -> transferService.transfer("origen@test.com", request));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la cuenta destino no existe")
    void transfer_DestAccountNotFound() {
        TransferRequest request = new TransferRequest("9999999999", BigDecimal.valueOf(50.00), "Test");

        when(usuarioRepository.findByEmail("origen@test.com")).thenReturn(Optional.of(userOrigen));
        when(cuentaRepository.findByUsuario(userOrigen)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transferService.transfer("origen@test.com", request));
    }
}
