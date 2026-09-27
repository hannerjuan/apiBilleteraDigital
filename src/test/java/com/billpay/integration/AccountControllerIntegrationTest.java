package com.billpay.integration;

import com.billpay.dto.request.DepositRequest;
import com.billpay.dto.request.RegisterRequest;
import com.billpay.dto.request.WithdrawRequest;
import com.billpay.dto.response.AuthResponse;
import com.billpay.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    private AuthResponse userAuth;

    @BeforeEach
    void setUp() {
        userAuth = authService.register(new RegisterRequest(
                "DOC_ACCOUNT_999",
                "Usuario Cuenta",
                "cuenta.test@banco.com",
                "Password123!",
                BigDecimal.valueOf(500.00)
        ));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/me - Obtiene saldo e información del titular autenticado")
    void getMyAccount_Success() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + userAuth.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta", is(userAuth.getNumeroCuenta())))
                .andExpect(jsonPath("$.saldo", is(500.00)))
                .andExpect(jsonPath("$.estado", is("ACTIVA")))
                .andExpect(jsonPath("$.titular", is("Usuario Cuenta")))
                .andExpect(jsonPath("$.email", is("cuenta.test@banco.com")));
    }

    @Test
    @DisplayName("POST /api/v1/accounts/deposit - Depósito exitoso acredita el saldo")
    void deposit_Success() throws Exception {
        DepositRequest depositReq = new DepositRequest(BigDecimal.valueOf(250.00), "Depósito ventanilla");

        mockMvc.perform(post("/api/v1/accounts/deposit")
                        .header("Authorization", "Bearer " + userAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto", is(250.00)))
                .andExpect(jsonPath("$.tipo", is("DEPOSITO")))
                .andExpect(jsonPath("$.estado", is("COMPLETADA")));

        // Verificar nuevo saldo
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + userAuth.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo", is(750.00)));
    }

    @Test
    @DisplayName("POST /api/v1/accounts/withdraw - Retiro exitoso debita el saldo")
    void withdraw_Success() throws Exception {
        WithdrawRequest withdrawReq = new WithdrawRequest(BigDecimal.valueOf(150.00), "Retiro cajero");

        mockMvc.perform(post("/api/v1/accounts/withdraw")
                        .header("Authorization", "Bearer " + userAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto", is(150.00)))
                .andExpect(jsonPath("$.tipo", is("RETIRO")))
                .andExpect(jsonPath("$.estado", is("COMPLETADA")));

        // Verificar nuevo saldo
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + userAuth.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo", is(350.00)));
    }

    @Test
    @DisplayName("POST /api/v1/accounts/withdraw - Rechaza retiro con fondos insuficientes con 422")
    void withdraw_InsufficientFunds_Returns422() throws Exception {
        WithdrawRequest withdrawReq = new WithdrawRequest(BigDecimal.valueOf(2000.00), "Retiro sin fondos");

        mockMvc.perform(post("/api/v1/accounts/withdraw")
                        .header("Authorization", "Bearer " + userAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", containsString("problem+json")))
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.title", is("Fondos insuficientes")));
    }
}
