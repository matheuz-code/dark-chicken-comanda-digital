package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.StatusPedidoCompra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedido_compra")
public class PedidoCompra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedidoCompra status = StatusPedidoCompra.RASCUNHO;

    @Column(name = "valor_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    protected PedidoCompra() {
    }

    public static PedidoCompra criar(Fornecedor fornecedor, Usuario usuario) {
        PedidoCompra pedido = new PedidoCompra();
        pedido.fornecedor = fornecedor;
        pedido.usuario = usuario;
        pedido.status = StatusPedidoCompra.RASCUNHO;
        pedido.valorTotal = BigDecimal.ZERO;
        return pedido;
    }

    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor fornecedor) { this.fornecedor = fornecedor; }
    public StatusPedidoCompra getStatus() { return status; }
    public void setStatus(StatusPedidoCompra status) { this.status = status; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
