package br.edu.unasp.comandadigital.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record IngredienteAdminResponse(
        Long id,
        String nome,
        String sku,
        UnidadeMedida unidadePadrao,
        BigDecimal estoqueMinimo,
        BigDecimal custoUnitario,
        StatusAtivacao status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
