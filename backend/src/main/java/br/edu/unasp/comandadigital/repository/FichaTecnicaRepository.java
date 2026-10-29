package br.edu.unasp.comandadigital.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import br.edu.unasp.comandadigital.entity.FichaTecnica;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {
    Optional<FichaTecnica> findByPratoId(Long pratoId);
    List<FichaTecnica> findByPratoIdIn(Collection<Long> pratoIds);
}
