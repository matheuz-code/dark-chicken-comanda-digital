package br.edu.unasp.comandadigital.dto.admin.compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.StatusPedidoCompra;

public record PedidoCompraResumoResponse(
        Long id,
        Long fornecedorId,
        String fornecedorRazaoSocial,
        StatusPedidoCompra status,
        BigDecimal valorTotal,
        Long usuarioId,
        String usuarioNome,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
