package br.edu.unasp.comandadigital.repository;

import br.edu.unasp.comandadigital.entity.Categoria;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Page<Categoria> findByStatus(StatusAtivacao status, Pageable pageable);
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
