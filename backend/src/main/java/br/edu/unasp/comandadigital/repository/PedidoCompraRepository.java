package br.edu.unasp.comandadigital.repository;

import br.edu.unasp.comandadigital.entity.PedidoCompra;
import br.edu.unasp.comandadigital.entity.enums.StatusPedidoCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {
    @EntityGraph(attributePaths = {"fornecedor", "usuario"})
    @Query("""
            SELECT pedido
            FROM PedidoCompra pedido
            WHERE (:status IS NULL OR pedido.status = :status)
              AND (:fornecedorId IS NULL OR pedido.fornecedor.id = :fornecedorId)
            """)
    Page<PedidoCompra> buscar(
            @Param("status") StatusPedidoCompra status,
            @Param("fornecedorId") Long fornecedorId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"fornecedor", "usuario"})
    @Query("SELECT pedido FROM PedidoCompra pedido WHERE pedido.id = :id")
    Optional<PedidoCompra> findDetalhadoById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"fornecedor", "usuario"})
    @Query("SELECT pedido FROM PedidoCompra pedido WHERE pedido.id = :id")
    Optional<PedidoCompra> findByIdForUpdate(@Param("id") Long id);
}
