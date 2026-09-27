-- PostgreSQL Schema Definition for BillPay Banking System

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    documento_identidad VARCHAR(20) NOT NULL UNIQUE,
    nombre_completo VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    fecha_registro TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    numero_cuenta VARCHAR(10) NOT NULL UNIQUE,
    saldo NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (saldo >= 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'BLOQUEADA', 'CANCELADA')),
    fecha_creacion TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transacciones (
    id BIGSERIAL PRIMARY KEY,
    id_cuenta_origen BIGINT REFERENCES cuentas(id),
    id_cuenta_destino BIGINT REFERENCES cuentas(id),
    monto NUMERIC(15, 2) NOT NULL CHECK (monto > 0),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('TRANSFERENCIA', 'DEPOSITO', 'RETIRO')),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('COMPLETADA', 'FALLIDA')),
    descripcion VARCHAR(255),
    fecha_creacion TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta_origen ON transacciones(id_cuenta_origen);
CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta_destino ON transacciones(id_cuenta_destino);
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha_creacion ON transacciones(fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_cuentas_numero_cuenta ON cuentas(numero_cuenta);
