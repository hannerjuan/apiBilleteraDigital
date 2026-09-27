package com.billpay.dto.response;

import com.billpay.model.EstadoTransaccion;
import com.billpay.model.TipoTransaccion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Respuesta detallada del movimiento o transacción bancaria")
public class TransactionResponse {

    @Schema(description = "Identificador único de la transacción", example = "101")
    private Long id;

    @Schema(description = "Número de cuenta origen (null para depósitos)", example = "1023456789")
    private String cuentaOrigen;

    @Schema(description = "Número de cuenta destino (null para retiros)", example = "1087654321")
    private String cuentaDestino;

    @Schema(description = "Monto monetario de la operación", example = "150.00")
    private BigDecimal monto;

    @Schema(description = "Tipo de operación", example = "TRANSFERENCIA")
    private TipoTransaccion tipo;

    @Schema(description = "Estado de la transacción", example = "COMPLETADA")
    private EstadoTransaccion estado;

    @Schema(description = "Detalle o concepto del movimiento", example = "Pago de servicios compartidos")
    private String descripcion;

    @Schema(description = "Fecha y hora exacta del registro del movimiento", example = "2026-09-27T10:30:00")
    private LocalDateTime fechaCreacion;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, String cuentaOrigen, String cuentaDestino, BigDecimal monto,
                               TipoTransaccion tipo, EstadoTransaccion estado, String descripcion, LocalDateTime fechaCreacion) {
        this.id = id;
        this.cuentaOrigen = cuentaOrigen;
        this.cuentaDestino = cuentaDestino;
        this.monto = monto;
        this.tipo = tipo;
        this.estado = estado;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCuentaOrigen() {
        return cuentaOrigen;
    }

    public void setCuentaOrigen(String cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
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

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransaccion tipo) {
        this.tipo = tipo;
    }

    public EstadoTransaccion getEstado() {
        return estado;
    }

    public void setEstado(EstadoTransaccion estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
