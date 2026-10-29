package br.edu.unasp.comandadigital.repository;

import java.util.List;

import br.edu.unasp.comandadigital.entity.PedidoCompraItem;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoCompraItemRepository extends JpaRepository<PedidoCompraItem, Long> {
    @EntityGraph(attributePaths = "ingrediente")
    List<PedidoCompraItem> findByPedidoCompraIdOrderById(Long pedidoCompraId);

    @EntityGraph(attributePaths = "ingrediente")
    List<PedidoCompraItem> findByPedidoCompraIdAndStatusOrderById(
            Long pedidoCompraId,
            StatusAtivacao status);
}
