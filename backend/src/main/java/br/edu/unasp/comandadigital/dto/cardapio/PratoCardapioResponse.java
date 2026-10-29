package br.edu.unasp.comandadigital.dto.cardapio;

import java.math.BigDecimal;

public record PratoCardapioResponse(
        Long id,
        String nome,
        String descricao,
        String fotoUrl,
        BigDecimal precoVenda,
        CategoriaCardapioResponse categoria) {
}
