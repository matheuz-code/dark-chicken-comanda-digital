package br.edu.unasp.comandadigital.repository;

import java.util.Collection;
import java.util.List;

import br.edu.unasp.comandadigital.entity.PedidoItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {
    @EntityGraph(attributePaths = "prato")
    List<PedidoItem> findByPedidoId(Long pedidoId);

    @Query("""
            SELECT
                item.pedido.id AS pedidoId,
                SUM(item.quantidade) AS quantidadeItens
            FROM PedidoItem item
            WHERE item.pedido.id IN :pedidoIds
            GROUP BY item.pedido.id
            """)
    List<PedidoQuantidadeItensProjection> somarQuantidadesPorPedido(
            @Param("pedidoIds") Collection<Long> pedidoIds);
}
