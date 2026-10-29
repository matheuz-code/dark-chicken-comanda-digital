package br.edu.unasp.comandadigital.repository;

import java.util.Collection;
import java.util.Optional;

import br.edu.unasp.comandadigital.entity.Pedido;
import br.edu.unasp.comandadigital.entity.enums.CanalPedido;
import br.edu.unasp.comandadigital.entity.enums.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "cliente")
    Page<Pedido> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "cliente")
    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);

    @EntityGraph(attributePaths = "cliente")
    Page<Pedido> findByCanal(CanalPedido canal, Pageable pageable);

    @EntityGraph(attributePaths = "cliente")
    Page<Pedido> findByStatusAndCanal(StatusPedido status, CanalPedido canal, Pageable pageable);

    @EntityGraph(attributePaths = "cliente")
    Page<Pedido> findByStatusIn(Collection<StatusPedido> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "cliente")
    Page<Pedido> findByStatusInAndCanal(
            Collection<StatusPedido> statuses,
            CanalPedido canal,
            Pageable pageable);

    Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);

    @EntityGraph(attributePaths = "cliente")
    @Query("SELECT pedido FROM Pedido pedido WHERE pedido.id = :id")
    Optional<Pedido> findComClienteById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "cliente")
    @Query("SELECT pedido FROM Pedido pedido WHERE pedido.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);
}
