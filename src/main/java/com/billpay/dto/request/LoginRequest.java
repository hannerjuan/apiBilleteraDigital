package com.billpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para inicio de sesión")
public class LoginRequest {

    @Schema(description = "Correo electrónico registrado", example = "carlos.perez@example.com")
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido.")
    private String email;

    @Schema(description = "Contraseña del usuario", example = "Password123!")
    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
