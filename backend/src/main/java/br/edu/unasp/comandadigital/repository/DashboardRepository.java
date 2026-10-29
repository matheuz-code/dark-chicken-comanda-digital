package br.edu.unasp.comandadigital.repository;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.unasp.comandadigital.entity.Pedido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DashboardRepository extends Repository<Pedido, Long> {

    @Query(value = """
            SELECT
                COUNT(*) AS totalPedidos,
                COALESCE(SUM(pedido.valor_total), 0) AS faturamento
            FROM pedido
            WHERE pedido.status_pagamento = 'PAGO'
              AND pedido.created_at >= :inicio
              AND pedido.created_at < :fim
            """, nativeQuery = true)
    DashboardResumoProjection resumirPedidosPagos(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query(value = """
            SELECT COALESCE(SUM(
                pedido_item.quantidade * COALESCE((
                    SELECT SUM(
                        CASE
                            WHEN ficha_item.unidade = ingrediente.unidade_padrao
                                THEN ficha_item.quantidade
                            WHEN ficha_item.unidade = 'KG' AND ingrediente.unidade_padrao = 'G'
                                THEN ficha_item.quantidade * 1000
                            WHEN ficha_item.unidade = 'G' AND ingrediente.unidade_padrao = 'KG'
                                THEN ficha_item.quantidade / 1000
                            WHEN ficha_item.unidade = 'L' AND ingrediente.unidade_padrao = 'ML'
                                THEN ficha_item.quantidade * 1000
                            WHEN ficha_item.unidade = 'ML' AND ingrediente.unidade_padrao = 'L'
                                THEN ficha_item.quantidade / 1000
                            ELSE 0
                        END
                        * ficha_item.fator_correcao
                        * ingrediente.custo_unitario
                    ) / NULLIF(MAX(ficha.rendimento), 0)
                    FROM ficha_tecnica ficha
                    JOIN ficha_tecnica_item ficha_item
                        ON ficha_item.ficha_tecnica_id = ficha.id
                       AND ficha_item.status = 'ATIVO'
                    JOIN ingrediente ingrediente ON ingrediente.id = ficha_item.ingrediente_id
                    WHERE ficha.prato_id = pedido_item.prato_id
                ), 0)
            ), 0) AS custoIngredientes
            FROM pedido_item
            JOIN pedido ON pedido.id = pedido_item.pedido_id
            WHERE pedido.status_pagamento = 'PAGO'
              AND pedido.created_at >= :inicio
              AND pedido.created_at < :fim
            """, nativeQuery = true)
    DashboardCustoProjection calcularCustoIngredientes(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query(value = """
            SELECT
                prato.id AS pratoId,
                prato.nome AS pratoNome,
                SUM(pedido_item.quantidade) AS quantidadeVendida,
                SUM(pedido_item.quantidade * pedido_item.preco_unitario) AS faturamento
            FROM pedido_item
            JOIN pedido ON pedido.id = pedido_item.pedido_id
            JOIN prato ON prato.id = pedido_item.prato_id
            WHERE pedido.status_pagamento = 'PAGO'
              AND pedido.created_at >= :inicio
              AND pedido.created_at < :fim
            GROUP BY prato.id, prato.nome
            ORDER BY quantidadeVendida DESC, faturamento DESC, prato.nome, prato.id
            """, nativeQuery = true)
    List<TopPratoProjection> buscarTopPratos(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);
}
