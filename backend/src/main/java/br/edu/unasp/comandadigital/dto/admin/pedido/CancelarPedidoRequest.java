package br.edu.unasp.comandadigital.dto.admin.pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarPedidoRequest(
        @NotBlank(message = "O motivo do cancelamento é obrigatório")
        @Size(max = 500, message = "O motivo do cancelamento deve ter no máximo 500 caracteres")
        String motivo) {
}
