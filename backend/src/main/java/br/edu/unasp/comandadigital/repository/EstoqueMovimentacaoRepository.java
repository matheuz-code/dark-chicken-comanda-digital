package br.edu.unasp.comandadigital.repository;

import java.util.Collection;
import java.util.List;

import br.edu.unasp.comandadigital.entity.EstoqueMovimentacao;
import br.edu.unasp.comandadigital.entity.enums.MotivoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.entity.enums.TipoMovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstoqueMovimentacaoRepository extends JpaRepository<EstoqueMovimentacao, Long> {
    Page<EstoqueMovimentacao> findByIngredienteId(Long ingredienteId, Pageable pageable);

    @EntityGraph(attributePaths = {"ingrediente", "usuario", "pedido", "pedidoCompra"})
    @Query("""
            SELECT movimentacao
            FROM EstoqueMovimentacao movimentacao
            WHERE (:ingredienteId IS NULL OR movimentacao.ingrediente.id = :ingredienteId)
              AND (:tipo IS NULL OR movimentacao.tipo = :tipo)
              AND (:motivo IS NULL OR movimentacao.motivo = :motivo)
            """)
    Page<EstoqueMovimentacao> buscarHistorico(
            @Param("ingredienteId") Long ingredienteId,
            @Param("tipo") TipoMovimentacaoEstoque tipo,
            @Param("motivo") MotivoMovimentacaoEstoque motivo,
            Pageable pageable);

    boolean existsByPedidoIdAndTipoAndMotivo(
            Long pedidoId,
            TipoMovimentacaoEstoque tipo,
            MotivoMovimentacaoEstoque motivo);

    @EntityGraph(attributePaths = "ingrediente")
    List<EstoqueMovimentacao> findByPedidoIdAndTipoAndMotivoOrderByIngredienteId(
            Long pedidoId,
            TipoMovimentacaoEstoque tipo,
            MotivoMovimentacaoEstoque motivo);

    @Query(value = """
            SELECT
                ingrediente_id AS ingredienteId,
                COALESCE(SUM(CASE
                    WHEN tipo IN ('ENTRADA', 'ESTORNO') THEN quantidade
                    ELSE -quantidade
                END), 0) AS saldo
            FROM estoque_movimentacao
            WHERE ingrediente_id IN (:ingredienteIds)
            GROUP BY ingrediente_id
            """, nativeQuery = true)
    List<SaldoEstoqueProjection> calcularSaldos(
            @Param("ingredienteIds") Collection<Long> ingredienteIds);
}
