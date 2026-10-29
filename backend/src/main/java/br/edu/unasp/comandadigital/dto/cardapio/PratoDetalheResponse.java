package br.edu.unasp.comandadigital.dto.cardapio;

import java.math.BigDecimal;

public record PratoDetalheResponse(
        Long id,
        String nome,
        String descricao,
        String fotoUrl,
        BigDecimal precoVenda,
        Integer tempoPreparoMin,
        CategoriaCardapioResponse categoria) {
}
