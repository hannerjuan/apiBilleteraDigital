package com.billpay.integration;

import com.billpay.dto.request.RegisterRequest;
import com.billpay.dto.request.TransferRequest;
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
class TransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    private AuthResponse userA;
    private AuthResponse userB;

    @BeforeEach
    void setUp() {
        userA = authService.register(new RegisterRequest(
                "DOC_A_123",
                "Usuario Emisor",
                "emisor@banco.com",
                "Password123!",
                BigDecimal.valueOf(1000.00)
        ));

        userB = authService.register(new RegisterRequest(
                "DOC_B_456",
                "Usuario Receptor",
                "receptor@banco.com",
                "Password123!",
                BigDecimal.valueOf(200.00)
        ));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Transferencia P2P exitosa con token Bearer")
    void transfer_Success() throws Exception {
        TransferRequest request = new TransferRequest(
                userB.getNumeroCuenta(),
                BigDecimal.valueOf(350.00),
                "Pago de alquiler"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userA.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.cuentaOrigen", is(userA.getNumeroCuenta())))
                .andExpect(jsonPath("$.cuentaDestino", is(userB.getNumeroCuenta())))
                .andExpect(jsonPath("$.monto", is(350.00)))
                .andExpect(jsonPath("$.tipo", is("TRANSFERENCIA")))
                .andExpect(jsonPath("$.estado", is("COMPLETADA")));

        // Verificar saldo actualizado del emisor con /api/v1/accounts/me
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + userA.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo", is(650.00)));

        // Verificar saldo actualizado del receptor con /api/v1/accounts/me
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + userB.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo", is(550.00)));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Sin autenticación retorna 401 Unauthorized y RFC 7807")
    void transfer_WithoutAuth_Returns401() throws Exception {
        TransferRequest request = new TransferRequest(
                userB.getNumeroCuenta(),
                BigDecimal.valueOf(100.00),
                "Test sin auth"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("problem+json")))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Fondos insuficientes retorna 422 Unprocessable Entity")
    void transfer_InsufficientFunds_Returns422() throws Exception {
        TransferRequest request = new TransferRequest(
                userB.getNumeroCuenta(),
                BigDecimal.valueOf(5000.00), // Excede los 1000 de saldo de userA
                "Monto mayor al saldo"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userA.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", containsString("problem+json")))
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.title", is("Fondos insuficientes")));
    }

    @Test
    @DisplayName("GET /api/v1/transfers/history - Retorna listado paginado de movimientos")
    void getHistory_Success() throws Exception {
        // Ejecutar una transferencia
        TransferRequest transfer = new TransferRequest(userB.getNumeroCuenta(), BigDecimal.valueOf(100.00), "Gasto");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + userA.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isOk());

        // Consultar historial
        mockMvc.perform(get("/api/v1/transfers/history")
                        .header("Authorization", "Bearer " + userA.getToken())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].cuentaOrigen", is(userA.getNumeroCuenta())))
                .andExpect(jsonPath("$.content[0].monto", is(100.00)));
    }
}
