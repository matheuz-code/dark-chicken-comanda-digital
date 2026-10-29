package br.edu.unasp.comandadigital.dto.admin.fornecedor;

import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;

public record FornecedorResponse(
        Long id,
        String razaoSocial,
        String cnpj,
        String telefone,
        String email,
        String categoriasProdutos,
        StatusAtivacao status,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
