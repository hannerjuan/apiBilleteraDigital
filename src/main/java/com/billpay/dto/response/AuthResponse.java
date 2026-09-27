package com.billpay.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de autenticación con token JWT")
public class AuthResponse {

    @Schema(description = "Token de acceso JWT en formato Bearer", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String token;

    @Schema(description = "Tipo de token", example = "Bearer")
    private String tipoToken = "Bearer";

    @Schema(description = "Correo electrónico del usuario autenticado", example = "carlos.perez@example.com")
    private String email;

    @Schema(description = "Nombre completo del usuario", example = "Carlos Andres Perez")
    private String nombreCompleto;

    @Schema(description = "Número de cuenta bancaria asignado (10 dígitos)", example = "1023456789")
    private String numeroCuenta;

    @Schema(description = "Tiempo de expiración del token en milisegundos", example = "86400000")
    private long expiresIn;

    public AuthResponse() {
    }

    public AuthResponse(String token, String email, String nombreCompleto, String numeroCuenta, long expiresIn) {
        this.token = token;
        this.tipoToken = "Bearer";
        this.email = email;
        this.nombreCompleto = nombreCompleto;
        this.numeroCuenta = numeroCuenta;
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipoToken() {
        return tipoToken;
    }

    public void setTipoToken(String tipoToken) {
        this.tipoToken = tipoToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
