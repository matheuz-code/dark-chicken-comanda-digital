package br.edu.unasp.comandadigital.repository;

import java.util.List;

import br.edu.unasp.comandadigital.entity.PedidoStatusHistorico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoStatusHistoricoRepository extends JpaRepository<PedidoStatusHistorico, Long> {
    Page<PedidoStatusHistorico> findByPedidoIdOrderByCreatedAtAsc(Long pedidoId, Pageable pageable);

    @EntityGraph(attributePaths = "usuario")
    List<PedidoStatusHistorico> findAllByPedidoIdOrderByCreatedAtAsc(Long pedidoId);
}
