package br.edu.unasp.comandadigital.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import br.edu.unasp.comandadigital.entity.FornecedorProduto;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FornecedorProdutoRepository extends JpaRepository<FornecedorProduto, Long> {
    @EntityGraph(attributePaths = {"fornecedor", "ingrediente"})
    Optional<FornecedorProduto> findByFornecedorIdAndIngredienteId(Long fornecedorId, Long ingredienteId);

    @EntityGraph(attributePaths = {"fornecedor", "ingrediente"})
    Optional<FornecedorProduto> findByIdAndFornecedorId(Long id, Long fornecedorId);

    @EntityGraph(attributePaths = {"fornecedor", "ingrediente"})
    Page<FornecedorProduto> findByFornecedorId(Long fornecedorId, Pageable pageable);

    @EntityGraph(attributePaths = {"fornecedor", "ingrediente"})
    Page<FornecedorProduto> findByFornecedorIdAndStatus(
            Long fornecedorId,
            StatusAtivacao status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"fornecedor", "ingrediente"})
    List<FornecedorProduto> findByIdIn(Collection<Long> ids);

    @Query(value = """
            SELECT produto.id
            FROM fornecedor_produto produto
            JOIN fornecedor fornecedor ON fornecedor.id = produto.fornecedor_id
            JOIN ingrediente ingrediente ON ingrediente.id = produto.ingrediente_id
            WHERE produto.ingrediente_id = :ingredienteId
              AND produto.status = 'ATIVO'
              AND fornecedor.status = 'ATIVO'
              AND ingrediente.status = 'ATIVO'
            ORDER BY CASE
                WHEN produto.unidade_venda = ingrediente.unidade_padrao THEN produto.preco
                WHEN produto.unidade_venda = 'KG' AND ingrediente.unidade_padrao = 'G'
                    THEN produto.preco / 1000
                WHEN produto.unidade_venda = 'G' AND ingrediente.unidade_padrao = 'KG'
                    THEN produto.preco * 1000
                WHEN produto.unidade_venda = 'L' AND ingrediente.unidade_padrao = 'ML'
                    THEN produto.preco / 1000
                WHEN produto.unidade_venda = 'ML' AND ingrediente.unidade_padrao = 'L'
                    THEN produto.preco * 1000
                ELSE produto.preco
            END, fornecedor.razao_social, produto.id
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM fornecedor_produto produto
            JOIN fornecedor fornecedor ON fornecedor.id = produto.fornecedor_id
            JOIN ingrediente ingrediente ON ingrediente.id = produto.ingrediente_id
            WHERE produto.ingrediente_id = :ingredienteId
              AND produto.status = 'ATIVO'
              AND fornecedor.status = 'ATIVO'
              AND ingrediente.status = 'ATIVO'
            """,
            nativeQuery = true)
    Page<Long> findIdsCotacaoAtiva(
            @Param("ingredienteId") Long ingredienteId,
            Pageable pageable);
}
