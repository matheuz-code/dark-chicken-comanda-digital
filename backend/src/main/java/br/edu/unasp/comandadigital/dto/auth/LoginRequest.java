package br.edu.unasp.comandadigital.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve ser válido")
        @Size(max = 180, message = "O email deve ter no máximo 180 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        String senha) {
}
