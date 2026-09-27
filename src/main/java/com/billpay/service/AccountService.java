package com.billpay.service;

import com.billpay.dto.request.DepositRequest;
import com.billpay.dto.request.WithdrawRequest;
import com.billpay.dto.response.AccountResponse;
import com.billpay.dto.response.TransactionResponse;
import com.billpay.exception.DailyLimitExceededException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Value("${app.banking.min-deposit-amount:1.00}")
    private BigDecimal minDepositAmount;

    @Value("${app.banking.max-deposit-amount:10000.00}")
    private BigDecimal maxDepositAmount;

    @Value("${app.banking.min-withdraw-amount:1.00}")
    private BigDecimal minWithdrawAmount;

    @Value("${app.banking.max-withdraw-amount:5000.00}")
    private BigDecimal maxWithdrawAmount;

    @Value("${app.banking.daily-withdrawal-limit:10000.00}")
    private BigDecimal dailyWithdrawalLimit;

    public AccountService(UsuarioRepository usuarioRepository,
                          CuentaRepository cuentaRepository,
                          TransaccionRepository transaccionRepository,
                          jakarta.persistence.EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
        this.entityManager = entityManager;
    }

    /**
     * Retrieves the current account details, real-time balance, and owner info for the authenticated user.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountByUser(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con correo: " + email));

        Cuenta cuenta = cuentaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cuenta asociada para el usuario."));

        return new AccountResponse(
                cuenta.getId(),
                cuenta.getNumeroCuenta(),
                cuenta.getSaldo(),
                cuenta.getEstado(),
                usuario.getNombreCompleto(),
                usuario.getDocumentoIdentidad(),
                usuario.getEmail(),
                cuenta.getFechaCreacion()
        );
    }

    /**
     * Executes an atomic deposit operation on the user's account.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse deposit(String userEmail, DepositRequest request) {
        BigDecimal amount = request.getMonto();

        if (amount == null || amount.compareTo(minDepositAmount) < 0) {
            throw new InvalidOperationException("El monto mínimo permitido para un depósito es $" + minDepositAmount);
        }
        if (amount.compareTo(maxDepositAmount) > 0) {
            throw new InvalidOperationException("El monto máximo permitido por depósito es $" + maxDepositAmount);
        }

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con correo: " + userEmail));

        Cuenta cuenta = cuentaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cuenta asociada para el usuario."));

        // Pessimistic lock on the account
        Cuenta lockedCuenta = cuentaRepository.findByIdWithLock(cuenta.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error al bloquear la cuenta para depósito."));
        if (entityManager != null) {
            entityManager.refresh(lockedCuenta);
        }

        if (!lockedCuenta.isActiva()) {
            throw new InactiveAccountException("La cuenta se encuentra en estado " + lockedCuenta.getEstado() + ". No admite depósitos.");
        }

        lockedCuenta.credit(amount);
        cuentaRepository.save(lockedCuenta);

        String concepto = request.getDescripcion() != null && !request.getDescripcion().isBlank()
                ? request.getDescripcion().trim()
                : "Depósito en cuenta bancaria";

        Transaccion transaccion = new Transaccion(
                null,
                lockedCuenta,
                amount,
                TipoTransaccion.DEPOSITO,
                EstadoTransaccion.COMPLETADA,
                concepto
        );
        Transaccion saved = transaccionRepository.save(transaccion);

        log.info("Depósito exitoso: Cuenta={}, Monto=${}", lockedCuenta.getNumeroCuenta(), amount);

        return new TransactionResponse(
                saved.getId(),
                null,
                lockedCuenta.getNumeroCuenta(),
                saved.getMonto(),
                saved.getTipo(),
                saved.getEstado(),
                saved.getDescripcion(),
                saved.getFechaCreacion()
        );
    }

    /**
     * Executes an atomic withdrawal operation from the user's account with daily limit validation.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse withdraw(String userEmail, WithdrawRequest request) {
        BigDecimal amount = request.getMonto();

        if (amount == null || amount.compareTo(minWithdrawAmount) < 0) {
            throw new InvalidOperationException("El monto mínimo permitido para un retiro es $" + minWithdrawAmount);
        }
        if (amount.compareTo(maxWithdrawAmount) > 0) {
            throw new InvalidOperationException("El monto máximo permitido por transacción de retiro es $" + maxWithdrawAmount);
        }

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con correo: " + userEmail));

        Cuenta cuenta = cuentaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cuenta asociada para el usuario."));

        // Pessimistic lock on the account
        Cuenta lockedCuenta = cuentaRepository.findByIdWithLock(cuenta.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Error al bloquear la cuenta para retiro."));
        if (entityManager != null) {
            entityManager.refresh(lockedCuenta);
        }

        if (!lockedCuenta.isActiva()) {
            throw new InactiveAccountException("La cuenta se encuentra en estado " + lockedCuenta.getEstado() + ". No admite retiros.");
        }

        // Validate daily limit
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        BigDecimal dailySum = transaccionRepository.sumAmountByCuentaOrigenAndTipoAndEstadoSince(
                lockedCuenta,
                TipoTransaccion.RETIRO,
                EstadoTransaccion.COMPLETADA,
                startOfDay
        );

        if (dailySum.add(amount).compareTo(dailyWithdrawalLimit) > 0) {
            BigDecimal disponibleDiario = dailyWithdrawalLimit.subtract(dailySum).max(BigDecimal.ZERO);
            throw new DailyLimitExceededException("Ha superado el límite diario de retiro de $" + dailyWithdrawalLimit +
                    ". Cupo restante hoy: $" + disponibleDiario);
        }

        // Validate sufficient funds
        if (lockedCuenta.getSaldo().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Fondos insuficientes. Su saldo disponible es $" + lockedCuenta.getSaldo() + " y el retiro solicitado es $" + amount);
        }

        lockedCuenta.debit(amount);
        cuentaRepository.save(lockedCuenta);

        String concepto = request.getDescripcion() != null && !request.getDescripcion().isBlank()
                ? request.getDescripcion().trim()
                : "Retiro de fondos en cajero o corresponsal";

        Transaccion transaccion = new Transaccion(
                lockedCuenta,
                null,
                amount,
                TipoTransaccion.RETIRO,
                EstadoTransaccion.COMPLETADA,
                concepto
        );
        Transaccion saved = transaccionRepository.save(transaccion);

        log.info("Retiro exitoso: Cuenta={}, Monto=${}", lockedCuenta.getNumeroCuenta(), amount);

        return new TransactionResponse(
                saved.getId(),
                lockedCuenta.getNumeroCuenta(),
                null,
                saved.getMonto(),
                saved.getTipo(),
                saved.getEstado(),
                saved.getDescripcion(),
                saved.getFechaCreacion()
        );
    }
}
