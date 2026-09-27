package com.billpay.integration;

import com.billpay.dto.request.LoginRequest;
import com.billpay.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/register - Registro exitoso retorna 201 y token JWT")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "CC987654321",
                "Maria Lopez",
                "maria.lopez@example.com",
                "Password123!",
                BigDecimal.valueOf(100.00)
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("maria.lopez@example.com")))
                .andExpect(jsonPath("$.nombreCompleto", is("Maria Lopez")))
                .andExpect(jsonPath("$.numeroCuenta", hasLength(10)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Rechaza datos inválidos con 400 y RFC 7807")
    void register_InvalidInput_ReturnsProblemDetail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "", // documento en blanco
                "M", // nombre muy corto
                "correo-invalido", // email invalido
                "123", // password debil
                BigDecimal.valueOf(-10) // saldo negativo
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("problem+json")))
                .andExpect(jsonPath("$.title", is("Error de validación de entrada")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.invalid_fields", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Autenticación exitosa retorna 200 y JWT")
    void login_Success() throws Exception {
        // Primero registrar
        RegisterRequest registerReq = new RegisterRequest(
                "CC12345678",
                "Carlos Sanchez",
                "carlos.sanchez@example.com",
                "Password123!",
                BigDecimal.valueOf(500.00)
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Luego login
        LoginRequest loginReq = new LoginRequest("carlos.sanchez@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("carlos.sanchez@example.com")))
                .andExpect(jsonPath("$.numeroCuenta", hasLength(10)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Credenciales incorrectas retorna 401 y RFC 7807")
    void login_InvalidCredentials_Returns401ProblemDetail() throws Exception {
        LoginRequest loginReq = new LoginRequest("noexiste@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("problem+json")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Autenticación fallida")));
    }
}
