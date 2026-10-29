package br.edu.unasp.comandadigital.repository;

import br.edu.unasp.comandadigital.entity.FornecedorProdutoPrecoHistorico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorProdutoPrecoHistoricoRepository
        extends JpaRepository<FornecedorProdutoPrecoHistorico, Long> {

    @EntityGraph(attributePaths = "usuario")
    Page<FornecedorProdutoPrecoHistorico> findByFornecedorProdutoId(
            Long fornecedorProdutoId,
            Pageable pageable);
}
