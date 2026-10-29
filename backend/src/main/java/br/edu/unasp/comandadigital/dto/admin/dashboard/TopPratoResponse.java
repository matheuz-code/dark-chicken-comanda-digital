package br.edu.unasp.comandadigital.dto.admin.dashboard;

import java.math.BigDecimal;

public record TopPratoResponse(
        Long pratoId,
        String pratoNome,
        long quantidadeVendida,
        BigDecimal faturamento) {
}
