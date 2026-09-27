package com.billpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Solicitud de depósito en cuenta")
public class DepositRequest {

    @Schema(description = "Monto a depositar", example = "200.00")
    @NotNull(message = "El monto a depositar es obligatorio.")
    @Positive(message = "El monto a depositar debe ser mayor a cero.")
    @Digits(integer = 13, fraction = 2, message = "El monto no puede tener más de 2 decimales.")
    private BigDecimal monto;

    @Schema(description = "Descripción o detalle del depósito", example = "Depósito en corresponsal bancario")
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres.")
    private String descripcion;

    public DepositRequest() {
    }

    public DepositRequest(BigDecimal monto, String descripcion) {
        this.monto = monto;
        this.descripcion = descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
