package br.edu.unasp.comandadigital.repository;

import br.edu.unasp.comandadigital.entity.Prato;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.StatusPrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PratoRepository extends JpaRepository<Prato, Long> {
    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findByStatus(StatusPrato status, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findByCategoriaId(Long categoriaId, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findByCategoriaIdAndStatus(Long categoriaId, StatusPrato status, Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    Page<Prato> findByStatusAndCategoriaStatus(
            StatusPrato status,
            StatusAtivacao categoriaStatus,
            Pageable pageable);
    Page<Prato> findByCategoriaIdAndStatusAndCategoriaStatus(
            Long categoriaId,
            StatusPrato status,
            StatusAtivacao categoriaStatus,
            Pageable pageable);
    Optional<Prato> findByIdAndStatusAndCategoriaStatus(
            Long id,
            StatusPrato status,
            StatusAtivacao categoriaStatus);
    List<Prato> findByIdInAndStatusAndCategoriaStatus(
            Collection<Long> ids,
            StatusPrato status,
            StatusAtivacao categoriaStatus);
    boolean existsByNomeIgnoreCaseAndCategoriaId(String nome, Long categoriaId);
    boolean existsByNomeIgnoreCaseAndCategoriaIdAndIdNot(
            String nome,
            Long categoriaId,
            Long id);
}
