package br.edu.unasp.comandadigital.dto.admin.fornecedor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;

public record CotacaoFornecedorResponse(
        Long fornecedorProdutoId,
        Long fornecedorId,
        String fornecedorRazaoSocial,
        String fornecedorCnpj,
        Long ingredienteId,
        String ingredienteNome,
        BigDecimal preco,
        UnidadeMedida unidadeVenda,
        BigDecimal precoNaUnidadePadrao,
        UnidadeMedida unidadePadrao,
        LocalDateTime atualizadoEm) {
}
