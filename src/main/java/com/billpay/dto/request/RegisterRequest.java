package com.billpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Solicitud de registro de cliente y apertura de cuenta")
public class RegisterRequest {

    @Schema(description = "Documento de identidad del cliente", example = "1098765432")
    @NotBlank(message = "El documento de identidad es obligatorio.")
    @Pattern(regexp = "^[0-9A-Za-z]{5,20}$", message = "El documento de identidad debe tener entre 5 y 20 caracteres alfanuméricos.")
    private String documentoIdentidad;

    @Schema(description = "Nombre completo del titular", example = "Carlos Andres Perez")
    @NotBlank(message = "El nombre completo es obligatorio.")
    @Size(min = 3, max = 150, message = "El nombre completo debe tener entre 3 y 150 caracteres.")
    private String nombreCompleto;

    @Schema(description = "Correo electrónico único", example = "carlos.perez@example.com")
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido.")
    @Size(max = 150, message = "El correo electrónico no puede exceder 150 caracteres.")
    private String email;

    @Schema(description = "Contraseña segura (mínimo 8 caracteres, al menos un dígito, una minúscula y una mayúscula)", example = "Password123!")
    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, max = 50, message = "La contraseña debe tener entre 8 y 50 caracteres.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$",
            message = "La contraseña debe tener al menos 8 caracteres y contener al menos un dígito, una letra minúscula y una mayúscula.")
    private String password;

    @Schema(description = "Saldo inicial de apertura voluntario (opcional, por defecto 0.00)", example = "500.00")
    @PositiveOrZero(message = "El saldo inicial debe ser cero o positivo.")
    @Digits(integer = 13, fraction = 2, message = "El saldo inicial no puede tener más de 2 decimales.")
    private BigDecimal saldoInicial;

    public RegisterRequest() {
    }

    public RegisterRequest(String documentoIdentidad, String nombreCompleto, String email, String password, BigDecimal saldoInicial) {
        this.documentoIdentidad = documentoIdentidad;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.password = password;
        this.saldoInicial = saldoInicial;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
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

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }
}
