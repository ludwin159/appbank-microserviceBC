package com.bank.appbank.controller;

import com.bank.appbank.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.ws.rs.QueryParam;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reportes", description = "Genera reportes de información.")
public class ReportsController {

    private final ReportService reportService;
    private final Clock clock;

    public ReportsController(ReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    @GetMapping("/report-average-balance-daily-by-client/{idClient}")
    @Operation(summary = "Generar reporte de saldo promedio diario por cliente",
            description =
            "El saldo promedio de cada producto incluye el monto actual de la cuenta si aún no se ha hecho el cierre.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Map<String, Object>> generateReportAverageBalanceDailyInPresentMonth(
            @Parameter(description = "ID del cliente") @PathVariable String idClient) {
        return reportService.generateReportAverageBalanceDailyInPresentMonth(idClient);
    }

    @GetMapping("/report-all-commissions")
    @Operation(summary = "Generar reporte de todas las comisiones",
            description = "Genera un reporte de todas las comisiones por producto en un rango de fechas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Map<String, Object>> generateReportAllCommissionByProduct(
            @Parameter(description = "Fecha inicial (formato YYYY-MM-DD)", required = true)
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final (formato YYYY-MM-DD)", required = true)
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.generateReportAllCommissionsByProductInRangeDate(from.toString(), to.toString());
    }

    @GetMapping("/report-general-by-bank-product")
    @Operation(summary = "Generar reporte genera de todos los productos bancarios",
            description = "Reporte de las cuentas existentes en el banco y las cantidades.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte generado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Map<String, Object>> generateReportCompleteAndGeneralByProductInRangeDate(
            @Parameter(description = "Fecha inicial (formato YYYY-MM-DD)", required = true)
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final (formato YYYY-MM-DD)", required = true)
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Instant fromInstant = from.atStartOfDay(clock.getZone()).toInstant();
        Instant toInstant = to.atTime(23, 59, 59).atZone(clock.getZone()).toInstant();
        return reportService.generateReportCompleteAndGeneralByProductInRangeDate(fromInstant, toInstant);
    }

    @GetMapping("/report-last-then-movements-cards-by-client/{idClient}")
    public Mono<Map<String, Object>> reportWithTheLastTenDebitAndCreditCardMovements (@PathVariable String idClient) {
        return reportService.reportLastTenMovementsCreditDebit(idClient);
    }

}
