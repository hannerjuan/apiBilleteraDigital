package com.billpay.service;

import com.billpay.dto.response.TransactionResponse;
import com.billpay.exception.ResourceNotFoundException;
import com.billpay.model.Cuenta;
import com.billpay.model.TipoTransaccion;
import com.billpay.model.Transaccion;
import com.billpay.model.Usuario;
import com.billpay.repository.CuentaRepository;
import com.billpay.repository.TransaccionRepository;
import com.billpay.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class TransactionService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;

    public TransactionService(UsuarioRepository usuarioRepository,
                              CuentaRepository cuentaRepository,
                              TransaccionRepository transaccionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
    }

    /**
     * Retrieves paginated transaction history for the authenticated user's account,
     * supporting filtering by date range (inclusive) and transaction type.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistory(String userEmail,
                                                TipoTransaccion tipo,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                Pageable pageable) {

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con correo: " + userEmail));

        Cuenta cuenta = cuentaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cuenta asociada para el usuario."));

        LocalDateTime startDateTime = startDate != null ? LocalDateTime.of(startDate, LocalTime.MIN) : null;
        LocalDateTime endDateTime = endDate != null ? LocalDateTime.of(endDate, LocalTime.MAX) : null;

        Page<Transaccion> transactions = transaccionRepository.findHistoryByCuentaAndFilters(
                cuenta,
                tipo,
                startDateTime,
                endDateTime,
                pageable
        );

        return transactions.map(this::mapToResponse);
    }

    private TransactionResponse mapToResponse(Transaccion t) {
        String cuentaOrigen = t.getCuentaOrigen() != null ? t.getCuentaOrigen().getNumeroCuenta() : null;
        String cuentaDestino = t.getCuentaDestino() != null ? t.getCuentaDestino().getNumeroCuenta() : null;

        return new TransactionResponse(
                t.getId(),
                cuentaOrigen,
                cuentaDestino,
                t.getMonto(),
                t.getTipo(),
                t.getEstado(),
                t.getDescripcion(),
                t.getFechaCreacion()
        );
    }
}
