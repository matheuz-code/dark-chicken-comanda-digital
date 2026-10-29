package br.edu.unasp.comandadigital.dto.admin.fornecedor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record HistoricoPrecoResponse(
        Long id,
        BigDecimal preco,
        UnidadeMedida unidadeVenda,
        BigDecimal precoNaUnidadePadrao,
        UnidadeMedida unidadePadrao,
        Long usuarioId,
        String usuarioNome,
        LocalDateTime registradoEm) {
}
