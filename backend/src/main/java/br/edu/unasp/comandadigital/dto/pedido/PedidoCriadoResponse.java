package br.edu.unasp.comandadigital.dto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.edu.unasp.comandadigital.entity.enums.FormaPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPedido;

public record PedidoCriadoResponse(
        Long id,
        StatusPedido status,
        StatusPagamento statusPagamento,
        FormaPagamento formaPagamento,
        BigDecimal valorTotal,
        String enderecoEntrega,
        String observacoes,
        LocalDateTime criadoEm,
        List<PedidoItemResponse> itens) {

    public PedidoCriadoResponse {
        itens = List.copyOf(itens);
    }
}
