package br.edu.unasp.comandadigital.repository;

import java.util.Optional;

import br.edu.unasp.comandadigital.entity.Fornecedor;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    Optional<Fornecedor> findByCnpj(String cnpj);
    Page<Fornecedor> findByStatus(StatusAtivacao status, Pageable pageable);
    boolean existsByCnpj(String cnpj);
    boolean existsByCnpjAndIdNot(String cnpj, Long id);
}
