package br.edu.unasp.comandadigital.dto.admin.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.CanalPedido;
import br.edu.unasp.comandadigital.entity.enums.FormaPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPagamento;
import br.edu.unasp.comandadigital.entity.enums.StatusPedido;

public record PedidoAdminResumoResponse(
        Long id,
        ClientePedidoAdminResponse cliente,
        CanalPedido canal,
        StatusPedido status,
        StatusPagamento statusPagamento,
        FormaPagamento formaPagamento,
        BigDecimal valorTotal,
        long quantidadeItens,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
