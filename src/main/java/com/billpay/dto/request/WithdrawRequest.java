package com.billpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Solicitud de retiro de fondos")
public class WithdrawRequest {

    @Schema(description = "Monto a retirar", example = "50.00")
    @NotNull(message = "El monto a retirar es obligatorio.")
    @Positive(message = "El monto a retirar debe ser mayor a cero.")
    @Digits(integer = 13, fraction = 2, message = "El monto no puede tener más de 2 decimales.")
    private BigDecimal monto;

    @Schema(description = "Descripción o detalle del retiro", example = "Retiro en cajero automático")
    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres.")
    private String descripcion;

    public WithdrawRequest() {
    }

    public WithdrawRequest(BigDecimal monto, String descripcion) {
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
