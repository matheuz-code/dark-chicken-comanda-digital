package br.edu.unasp.comandadigital.dto.admin.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.edu.unasp.comandadigital.entity.enums.CanalPedido;
import br.edu.unasp.comandadigital.entity.enums.FormaPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPedido;

public record PedidoAdminDetalheResponse(
        Long id,
        ClientePedidoAdminResponse cliente,
        CanalPedido canal,
        StatusPedido status,
        StatusPagamento statusPagamento,
        FormaPagamento formaPagamento,
        BigDecimal valorTotal,
        String enderecoEntrega,
        String observacoes,
        String motivoCancelamento,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        List<PedidoAdminItemResponse> itens,
        List<PedidoAdminHistoricoResponse> timeline) {

    public PedidoAdminDetalheResponse {
        itens = List.copyOf(itens);
        timeline = List.copyOf(timeline);
    }
}
