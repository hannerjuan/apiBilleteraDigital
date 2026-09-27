package com.billpay.repository;

import com.billpay.model.Cuenta;
import com.billpay.model.EstadoTransaccion;
import com.billpay.model.TipoTransaccion;
import com.billpay.model.Transaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long>, JpaSpecificationExecutor<Transaccion> {

    /**
     * Finds transactions associated with a given account (either origin or destination)
     * with optional filtering by date range and transaction type.
     */
    @Query("""
        SELECT t FROM Transaccion t
        WHERE (t.cuentaOrigen = :cuenta OR t.cuentaDestino = :cuenta)
          AND (:tipo IS NULL OR t.tipo = :tipo)
          AND (:startDate IS NULL OR t.fechaCreacion >= :startDate)
          AND (:endDate IS NULL OR t.fechaCreacion <= :endDate)
        ORDER BY t.fechaCreacion DESC
    """)
    Page<Transaccion> findHistoryByCuentaAndFilters(
            @Param("cuenta") Cuenta cuenta,
            @Param("tipo") TipoTransaccion tipo,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Calculates the sum of transactions for a specific account, type, and status since a given time (e.g. today).
     */
    @Query("""
        SELECT COALESCE(SUM(t.monto), 0)
        FROM Transaccion t
        WHERE t.cuentaOrigen = :cuenta
          AND t.tipo = :tipo
          AND t.estado = :estado
          AND t.fechaCreacion >= :since
    """)
    BigDecimal sumAmountByCuentaOrigenAndTipoAndEstadoSince(
            @Param("cuenta") Cuenta cuenta,
            @Param("tipo") TipoTransaccion tipo,
            @Param("estado") EstadoTransaccion estado,
            @Param("since") LocalDateTime since
    );
}
