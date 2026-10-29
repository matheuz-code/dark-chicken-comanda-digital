package br.edu.unasp.comandadigital.dto.admin.fornecedor;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.validation.Cnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
        @NotBlank(message = "A razão social é obrigatória")
        @Size(max = 180, message = "A razão social deve ter no máximo 180 caracteres")
        String razaoSocial,

        @NotBlank(message = "O CNPJ é obrigatório")
        @Size(max = 18, message = "O CNPJ deve ter no máximo 18 caracteres")
        @Cnpj
        String cnpj,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String telefone,

        @Email(message = "O e-mail do fornecedor deve ser válido")
        @Size(max = 180, message = "O e-mail deve ter no máximo 180 caracteres")
        String email,

        @Size(max = 500, message = "As categorias de produtos devem ter no máximo 500 caracteres")
        String categoriasProdutos,

        @NotNull(message = "O status do fornecedor é obrigatório")
        StatusAtivacao status) {
}
