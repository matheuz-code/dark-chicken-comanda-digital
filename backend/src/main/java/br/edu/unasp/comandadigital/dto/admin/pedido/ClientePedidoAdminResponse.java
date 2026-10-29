package br.edu.unasp.comandadigital.dto.admin.pedido;

public record ClientePedidoAdminResponse(
        Long id,
        String nome,
        String email,
        String telefone) {
}
