package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "pedido_compra_item", uniqueConstraints = @UniqueConstraint(
        name = "uk_pedido_compra_item", columnNames = {"pedido_compra_id", "ingrediente_id"}))
public class PedidoCompraItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_compra_id", nullable = false)
    private PedidoCompra pedidoCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal precoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_compra", nullable = false, length = 10)
    private UnidadeMedida unidadeCompra;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtivacao status = StatusAtivacao.ATIVO;

    protected PedidoCompraItem() {
    }

    public static PedidoCompraItem criar(
            PedidoCompra pedidoCompra,
            Ingrediente ingrediente,
            BigDecimal quantidade,
            BigDecimal precoUnitario,
            UnidadeMedida unidadeCompra,
            BigDecimal subtotal) {
        PedidoCompraItem item = new PedidoCompraItem();
        item.pedidoCompra = pedidoCompra;
        item.ingrediente = ingrediente;
        item.quantidade = quantidade;
        item.precoUnitario = precoUnitario;
        item.unidadeCompra = unidadeCompra;
        item.subtotal = subtotal;
        item.status = StatusAtivacao.ATIVO;
        return item;
    }

    public PedidoCompra getPedidoCompra() { return pedidoCompra; }
    public void setPedidoCompra(PedidoCompra pedidoCompra) { this.pedidoCompra = pedidoCompra; }
    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public UnidadeMedida getUnidadeCompra() { return unidadeCompra; }
    public void setUnidadeCompra(UnidadeMedida unidadeCompra) { this.unidadeCompra = unidadeCompra; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public StatusAtivacao getStatus() { return status; }
    public void setStatus(StatusAtivacao status) { this.status = status; }
}
