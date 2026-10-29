package br.edu.unasp.comandadigital.repository;

import java.util.Collection;
import java.util.List;

import br.edu.unasp.comandadigital.entity.FichaTecnicaItem;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaTecnicaItemRepository extends JpaRepository<FichaTecnicaItem, Long> {
    @EntityGraph(attributePaths = "ingrediente")
    List<FichaTecnicaItem> findByFichaTecnicaId(Long fichaTecnicaId);
    boolean existsByFichaTecnicaIdAndStatus(Long fichaTecnicaId, StatusAtivacao status);

    @EntityGraph(attributePaths = "ingrediente")
    List<FichaTecnicaItem> findByFichaTecnicaIdAndStatus(
            Long fichaTecnicaId,
            StatusAtivacao status);

    @EntityGraph(attributePaths = "ingrediente")
    List<FichaTecnicaItem> findByFichaTecnicaIdInAndStatus(
            Collection<Long> fichaTecnicaIds,
            StatusAtivacao status);
}
