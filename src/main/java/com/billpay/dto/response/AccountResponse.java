package com.billpay.dto.response;

import com.billpay.model.EstadoCuenta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Información detallada de la cuenta bancaria")
public class AccountResponse {

    @Schema(description = "Identificador único de la cuenta", example = "1")
    private Long id;

    @Schema(description = "Número único de cuenta (10 dígitos)", example = "1023456789")
    private String numeroCuenta;

    @Schema(description = "Saldo disponible en tiempo real", example = "1250.75")
    private BigDecimal saldo;

    @Schema(description = "Estado actual de la cuenta", example = "ACTIVA")
    private EstadoCuenta estado;

    @Schema(description = "Nombre completo del titular", example = "Carlos Andres Perez")
    private String titular;

    @Schema(description = "Documento de identidad del titular", example = "1098765432")
    private String documentoIdentidad;

    @Schema(description = "Correo electrónico del titular", example = "carlos.perez@example.com")
    private String email;

    @Schema(description = "Fecha y hora de creación de la cuenta", example = "2026-09-27T10:00:00")
    private LocalDateTime fechaCreacion;

    public AccountResponse() {
    }

    public AccountResponse(Long id, String numeroCuenta, BigDecimal saldo, EstadoCuenta estado,
                           String titular, String documentoIdentidad, String email, LocalDateTime fechaCreacion) {
        this.id = id;
        this.numeroCuenta = numeroCuenta;
        this.saldo = saldo;
        this.estado = estado;
        this.titular = titular;
        this.documentoIdentidad = documentoIdentidad;
        this.email = email;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoCuenta estado) {
        this.estado = estado;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
