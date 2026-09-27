package com.billpay.controller;

import com.billpay.dto.request.TransferRequest;
import com.billpay.dto.response.TransactionResponse;
import com.billpay.model.TipoTransaccion;
import com.billpay.service.TransactionService;
import com.billpay.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transferencias y Movimientos", description = "Endpoints para transferencias entre cuentas P2P e historial de transacciones")
@SecurityRequirement(name = "Bearer Authentication")
public class TransferController {

    private final TransferService transferService;
    private final TransactionService transactionService;

    public TransferController(TransferService transferService, TransactionService transactionService) {
        this.transferService = transferService;
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Ejecución de una transferencia atómica entre dos cuentas (P2P)",
            description = "Transfiere fondos de forma atómica y consistente con bloqueo pesimista en base de datos. Deduce y acredita simultáneamente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transferencia ejecutada exitosamente",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Monto inválido, cuenta destino mal formada o transferencia hacia la misma cuenta",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autorizado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Cuenta destino no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Fondos insuficientes o cuenta de origen/destino inactiva o bloqueada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TransactionResponse> transfer(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transferService.transfer(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Listado paginado de movimientos con filtros por rango de fechas y tipo",
            description = "Consulta el historial de ingresos y egresos de la cuenta del usuario autenticado con soporte de paginación y filtros.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial paginado de movimientos obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Page<TransactionResponse>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Tipo de operación (TRANSFERENCIA, DEPOSITO, RETIRO)")
            @RequestParam(required = false) TipoTransaccion tipo,
            @Parameter(description = "Fecha inicial (formato YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Fecha final (formato YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Page<TransactionResponse> history = transactionService.getHistory(
                userDetails.getUsername(),
                tipo,
                startDate,
                endDate,
                pageable
        );
        return ResponseEntity.ok(history);
    }
}
