package br.edu.unasp.comandadigital.dto.pedido;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.unasp.comandadigital.entity.enums.StatusPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPedido;

public record PedidoStatusResponse(
        Long id,
        StatusPedido status,
        StatusPagamento statusPagamento,
        LocalDateTime atualizadoEm,
        List<PedidoStatusHistoricoResponse> timeline) {

    public PedidoStatusResponse {
        timeline = List.copyOf(timeline);
    }
}
