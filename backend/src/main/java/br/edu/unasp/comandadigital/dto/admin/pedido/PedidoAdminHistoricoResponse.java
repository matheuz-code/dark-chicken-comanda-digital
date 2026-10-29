package br.edu.unasp.comandadigital.dto.admin.pedido;

import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.StatusPedido;

public record PedidoAdminHistoricoResponse(
        StatusPedido status,
        String observacao,
        UsuarioHistoricoAdminResponse usuario,
        LocalDateTime registradoEm) {
}
