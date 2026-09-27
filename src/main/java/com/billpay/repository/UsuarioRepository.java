package com.billpay.repository;

import com.billpay.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByDocumentoIdentidad(String documentoIdentidad);

    boolean existsByEmail(String email);

    boolean existsByDocumentoIdentidad(String documentoIdentidad);
}
