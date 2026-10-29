package br.edu.unasp.comandadigital.dto.admin.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.edu.unasp.comandadigital.entity.enums.StatusPedidoCompra;

public record PedidoCompraDetalheResponse(
        Long id,
        Long fornecedorId,
        String fornecedorRazaoSocial,
        String fornecedorCnpj,
        StatusPedidoCompra status,
        BigDecimal valorTotal,
        Long usuarioId,
        String usuarioNome,
        List<PedidoCompraItemResponse> itens,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public PedidoCompraDetalheResponse {
        itens = List.copyOf(itens);
    }
}
