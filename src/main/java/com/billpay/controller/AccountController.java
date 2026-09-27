package com.billpay.controller;

import com.billpay.dto.request.DepositRequest;
import com.billpay.dto.request.WithdrawRequest;
import com.billpay.dto.response.AccountResponse;
import com.billpay.dto.response.TransactionResponse;
import com.billpay.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Cuentas Financieras", description = "Operaciones sobre cuentas bancarias: consulta de saldo en tiempo real, depósitos y retiros")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consulta de saldo y estado de la cuenta del usuario autenticado",
            description = "Devuelve el balance en tiempo real, estado de la cuenta y datos del titular autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos de la cuenta obtenidos exitosamente",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AccountResponse> getMyAccount(@AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse response = accountService.getAccountByUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    @Operation(summary = "Depósito de saldo (fondeo simulado en corresponsal/cajero)",
            description = "Acredita saldo de manera atómica con bloqueo pesimista en la cuenta del usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Depósito efectuado correctamente",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Monto inválido o fuera de rango",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Cuenta inactiva o bloqueada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TransactionResponse> deposit(@AuthenticationPrincipal UserDetails userDetails,
                                                       @Valid @RequestBody DepositRequest request) {
        TransactionResponse response = accountService.deposit(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Retiro de saldo (extracción simulada en cajero automático)",
            description = "Debita fondos de forma atómica con bloqueo pesimista, validando límites diarios y fondos disponibles.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retiro efectuado correctamente",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Monto inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Fondos insuficientes, límite diario excedido o cuenta inactiva",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TransactionResponse> withdraw(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody WithdrawRequest request) {
        TransactionResponse response = accountService.withdraw(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
}
