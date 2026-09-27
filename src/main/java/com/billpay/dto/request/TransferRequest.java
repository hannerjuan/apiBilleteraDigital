package com.billpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Solicitud de transferencia entre cuentas P2P")
public class TransferRequest {

    @Schema(description = "Número de cuenta de ahorros destino (10 dígitos numéricos)", example = "1002345678")
    @NotBlank(message = "El número de cuenta destino es obligatorio.")
    @Pattern(regexp = "^\\d{10}$", message = "El número de cuenta destino debe tener exactamente 10 dígitos numéricos.")
    private String cuentaDestino;

    @Schema(description = "Monto monetario a transferir", example = "150.00")
    @NotNull(message = "El monto de la transferencia es obligatorio.")
    @Positive(message = "El monto a transferir debe ser mayor a cero.")
    @Digits(integer = 13, fraction = 2, message = "El monto no puede contener más de 2 decimales.")
    private BigDecimal monto;

    @Schema(description = "Concepto o descripción opcional del movimiento", example = "Pago de servicios compartidos")
    @Size(max = 255, message = "La descripción no puede exceder los 255 caracteres.")
    private String descripcion;

    public TransferRequest() {
    }

    public TransferRequest(String cuentaDestino, BigDecimal monto, String descripcion) {
        this.cuentaDestino = cuentaDestino;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public void setCuentaDestino(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
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
