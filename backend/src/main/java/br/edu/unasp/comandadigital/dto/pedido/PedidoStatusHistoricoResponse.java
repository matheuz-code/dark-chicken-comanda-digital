package br.edu.unasp.comandadigital.dto.pedido;

import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.StatusPedido;

public record PedidoStatusHistoricoResponse(
        StatusPedido status,
        String observacao,
        LocalDateTime registradoEm) {
}
