package br.edu.unasp.comandadigital.dto.admin.pedido;

import br.edu.unasp.comandadigital.entity.enums.StatusPedido;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarStatusPedidoRequest(
        @NotNull(message = "O novo status é obrigatório")
        StatusPedido status,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao) {
}
