package com.billpay.service;

import com.billpay.dto.request.TransferRequest;
import com.billpay.dto.response.TransactionResponse;
import com.billpay.exception.InactiveAccountException;
import com.billpay.exception.InsufficientFundsException;
import com.billpay.exception.InvalidOperationException;
import com.billpay.exception.ResourceNotFoundException;
import com.billpay.model.Cuenta;
import com.billpay.model.EstadoTransaccion;
import com.billpay.model.TipoTransaccion;
import com.billpay.model.Transaccion;
import com.billpay.model.Usuario;
import com.billpay.repository.CuentaRepository;
import com.billpay.repository.TransaccionRepository;
import com.billpay.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Value("${app.banking.min-transfer-amount:1.00}")
    private BigDecimal minTransferAmount;

    public TransferService(UsuarioRepository usuarioRepository,
                           CuentaRepository cuentaRepository,
                           TransaccionRepository transaccionRepository,
                           jakarta.persistence.EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
        this.entityManager = entityManager;
    }

    /**
     * Executes an atomic, ACID-compliant P2P money transfer between origin and destination accounts.
     * Implements database-level pessimistic locking (PESSIMISTIC_WRITE) and ordered lock acquisition
     * to completely eliminate race conditions, double spending, and database deadlocks.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transfer(String userEmail, TransferRequest request) {
        BigDecimal amount = request.getMonto();

        // 1. Validar monto mínimo
        if (amount == null || amount.compareTo(minTransferAmount) < 0) {
            throw new InvalidOperationException("El monto mínimo permitido para una transferencia es $" + minTransferAmount);
        }

        // 2. Obtener usuario emisor y su cuenta asociada
        Usuario usuarioEmisor = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con correo: " + userEmail));

        Cuenta cuentaEmisor = cuentaRepository.findByUsuario(usuarioEmisor)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cuenta activa para el usuario autenticado."));

        String numeroCuentaOrigen = cuentaEmisor.getNumeroCuenta();
        String numeroCuentaDestino = request.getCuentaDestino().trim();

        // 3. Validar que la cuenta origen no sea igual a la cuenta destino
        if (numeroCuentaOrigen.equals(numeroCuentaDestino)) {
            throw new InvalidOperationException("No se permite realizar transferencias hacia la misma cuenta de origen.");
        }

        // 4. Verificar existencia preliminar de la cuenta destino
        Cuenta cuentaDestinoPrev = cuentaRepository.findByNumeroCuenta(numeroCuentaDestino)
                .orElseThrow(() -> new ResourceNotFoundException("La cuenta de ahorros destino " + numeroCuentaDestino + " no existe."));

        // 5. Adquisición ordenada de bloqueos pesimistas (Deadlock Prevention Strategy)
        // Bloquear siempre las cuentas en orden ascendente de ID de base de datos
        Long idOrigen = cuentaEmisor.getId();
        Long idDestino = cuentaDestinoPrev.getId();

        Long firstId = idOrigen.compareTo(idDestino) < 0 ? idOrigen : idDestino;
        Long secondId = idOrigen.compareTo(idDestino) < 0 ? idDestino : idOrigen;

        Cuenta firstLocked = cuentaRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Error al adquirir bloqueo pesimista en la cuenta con ID: " + firstId));
        Cuenta secondLocked = cuentaRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Error al adquirir bloqueo pesimista en la cuenta con ID: " + secondId));

        Cuenta lockedOrigen = idOrigen.equals(firstLocked.getId()) ? firstLocked : secondLocked;
        Cuenta lockedDestino = idDestino.equals(firstLocked.getId()) ? firstLocked : secondLocked;

        if (entityManager != null) {
            entityManager.refresh(lockedOrigen);
            entityManager.refresh(lockedDestino);
        }

        // 6. Validar estado de la cuenta origen
        if (!lockedOrigen.isActiva()) {
            throw new InactiveAccountException("La cuenta de origen se encuentra en estado " + lockedOrigen.getEstado() + ". No puede transferir fondos.");
        }

        // 7. Validar estado de la cuenta destino
        if (!lockedDestino.isActiva()) {
            throw new InactiveAccountException("La cuenta de destino se encuentra en estado " + lockedDestino.getEstado() + ". No puede recibir transferencias.");
        }

        // 8. Validar balance suficiente en cuenta origen (Prevención de doble gasto)
        if (lockedOrigen.getSaldo().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Fondos insuficientes. Su saldo disponible es de $" + lockedOrigen.getSaldo() + " y el monto solicitado es $" + amount);
        }

        // 9. Ejecutar débito y crédito simultáneos
        lockedOrigen.debit(amount);
        lockedDestino.credit(amount);

        cuentaRepository.save(lockedOrigen);
        cuentaRepository.save(lockedDestino);

        // 10. Registrar movimiento de transacción
        String concepto = request.getDescripcion() != null && !request.getDescripcion().isBlank()
                ? request.getDescripcion().trim()
                : "Transferencia entre cuentas de ahorros";

        Transaccion transaccion = new Transaccion(
                lockedOrigen,
                lockedDestino,
                amount,
                TipoTransaccion.TRANSFERENCIA,
                EstadoTransaccion.COMPLETADA,
                concepto
        );

        Transaccion savedTransaccion = transaccionRepository.save(transaccion);

        log.info("Transferencia exitosa: ID={}, Origen={}, Destino={}, Monto=${}",
                savedTransaccion.getId(), lockedOrigen.getNumeroCuenta(), lockedDestino.getNumeroCuenta(), amount);

        return new TransactionResponse(
                savedTransaccion.getId(),
                lockedOrigen.getNumeroCuenta(),
                lockedDestino.getNumeroCuenta(),
                savedTransaccion.getMonto(),
                savedTransaccion.getTipo(),
                savedTransaccion.getEstado(),
                savedTransaccion.getDescripcion(),
                savedTransaccion.getFechaCreacion()
        );
    }
}
