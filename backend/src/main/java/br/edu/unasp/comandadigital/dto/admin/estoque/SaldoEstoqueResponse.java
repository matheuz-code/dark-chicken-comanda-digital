package br.edu.unasp.comandadigital.dto.admin.estoque;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record SaldoEstoqueResponse(
        Long ingredienteId,
        String nome,
        String sku,
        UnidadeMedida unidadePadrao,
        BigDecimal saldoAtual,
        BigDecimal estoqueMinimo,
        boolean abaixoDoMinimo,
        StatusAtivacao status) {
}
