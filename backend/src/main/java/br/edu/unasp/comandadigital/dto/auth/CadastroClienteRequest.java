package br.edu.unasp.comandadigital.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve ser válido")
        @Size(max = 180, message = "O email deve ter no máximo 180 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
        String senha,

        @NotBlank(message = "O telefone é obrigatório")
        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String telefone,

        @NotBlank(message = "O endereço de entrega é obrigatório")
        @Size(max = 500, message = "O endereço deve ter no máximo 500 caracteres")
        String endereco) {
}
