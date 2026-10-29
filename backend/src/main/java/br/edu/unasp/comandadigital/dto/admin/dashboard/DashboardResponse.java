package br.edu.unasp.comandadigital.dto.admin.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardResponse(
        LocalDate data,
        BigDecimal faturamento,
        long totalPedidos,
        BigDecimal ticketMedio,
        BigDecimal custoIngredientesEstimado,
        BigDecimal foodCostMedioPercentual) {
}
