package com.billpay.repository;

import com.billpay.model.Cuenta;
import com.billpay.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    Optional<Cuenta> findByUsuario(Usuario usuario);

    Optional<Cuenta> findByUsuarioId(Long usuarioId);

    boolean existsByNumeroCuenta(String numeroCuenta);

    /**
     * Pessimistic lock on account by account number to prevent race conditions and double spending.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> findByNumeroCuentaWithLock(@Param("numeroCuenta") String numeroCuenta);

    /**
     * Pessimistic lock on account by ID to ensure deadlock-free ordered locking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.id = :id")
    Optional<Cuenta> findByIdWithLock(@Param("id") Long id);
}
