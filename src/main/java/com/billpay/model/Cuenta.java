package com.billpay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "numero_cuenta", nullable = false, unique = true, length = 10)
    private String numeroCuenta;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public Cuenta() {
    }

    public Cuenta(Usuario usuario, String numeroCuenta, BigDecimal saldo, EstadoCuenta estado) {
        this.usuario = usuario;
        this.numeroCuenta = numeroCuenta;
        this.saldo = saldo != null ? saldo : BigDecimal.ZERO;
        this.estado = estado != null ? estado : EstadoCuenta.ACTIVA;
        this.fechaCreacion = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.saldo == null) {
            this.saldo = BigDecimal.ZERO;
        }
        if (this.estado == null) {
            this.estado = EstadoCuenta.ACTIVA;
        }
    }

    public boolean isActiva() {
        return EstadoCuenta.ACTIVA.equals(this.estado);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a debitar debe ser positivo.");
        }
        if (this.saldo.compareTo(amount) < 0) {
            throw new IllegalStateException("Fondos insuficientes para realizar el débito.");
        }
        this.saldo = this.saldo.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a acreditar debe ser positivo.");
        }
        this.saldo = this.saldo.add(amount);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cuenta cuenta = (Cuenta) o;
        return Objects.equals(id, cuenta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
