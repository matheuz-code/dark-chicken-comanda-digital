package br.edu.unasp.comandadigital.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import br.edu.unasp.comandadigital.entity.Ingrediente;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {
    Optional<Ingrediente> findBySkuIgnoreCase(String sku);
    Page<Ingrediente> findByStatus(StatusAtivacao status, Pageable pageable);
    List<Ingrediente> findByIdInAndStatus(
            Collection<Long> ids,
            StatusAtivacao status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ingrediente
            FROM Ingrediente ingrediente
            WHERE ingrediente.id IN :ids
            ORDER BY ingrediente.id
            """)
    List<Ingrediente> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Query(value = """
            SELECT ingrediente.id
            FROM ingrediente
            WHERE ingrediente.status = 'ATIVO'
              AND COALESCE((
                  SELECT SUM(CASE
                      WHEN movimentacao.tipo IN ('ENTRADA', 'ESTORNO')
                          THEN movimentacao.quantidade
                      ELSE -movimentacao.quantidade
                  END)
                  FROM estoque_movimentacao movimentacao
                  WHERE movimentacao.ingrediente_id = ingrediente.id
              ), 0) < ingrediente.estoque_minimo
            ORDER BY ingrediente.nome, ingrediente.id
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM ingrediente
            WHERE ingrediente.status = 'ATIVO'
              AND COALESCE((
                  SELECT SUM(CASE
                      WHEN movimentacao.tipo IN ('ENTRADA', 'ESTORNO')
                          THEN movimentacao.quantidade
                      ELSE -movimentacao.quantidade
                  END)
                  FROM estoque_movimentacao movimentacao
                  WHERE movimentacao.ingrediente_id = ingrediente.id
              ), 0) < ingrediente.estoque_minimo
            """,
            nativeQuery = true)
    Page<Long> findIdsAbaixoDoEstoqueMinimo(Pageable pageable);

    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
}
