package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.unasp.comandadigital.entity.enums.MotivoMovimentacaoEstoque;
import br.edu.unasp.comandadigital.entity.enums.TipoMovimentacaoEstoque;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "estoque_movimentacao")
public class EstoqueMovimentacao extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoMovimentacaoEstoque motivo;

    @Column(length = 100)
    private String lote;

    private LocalDate validade;

    @Column(name = "custo_unitario", precision = 14, scale = 4)
    private BigDecimal custoUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_compra_id")
    private PedidoCompra pedidoCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    protected EstoqueMovimentacao() {
    }

    public static EstoqueMovimentacao saidaVenda(
            Ingrediente ingrediente,
            BigDecimal quantidade,
            Pedido pedido,
            Usuario usuario) {
        EstoqueMovimentacao movimentacao = new EstoqueMovimentacao();
        movimentacao.ingrediente = ingrediente;
        movimentacao.tipo = TipoMovimentacaoEstoque.SAIDA;
        movimentacao.quantidade = quantidade;
        movimentacao.motivo = MotivoMovimentacaoEstoque.VENDA;
        movimentacao.lote = "PEDIDO-" + pedido.getId();
        movimentacao.custoUnitario = ingrediente.getCustoUnitario();
        movimentacao.pedido = pedido;
        movimentacao.usuario = usuario;
        return movimentacao;
    }

    public static EstoqueMovimentacao estornoVenda(
            EstoqueMovimentacao saida,
            Pedido pedido,
            Usuario usuario) {
        EstoqueMovimentacao movimentacao = new EstoqueMovimentacao();
        movimentacao.ingrediente = saida.getIngrediente();
        movimentacao.tipo = TipoMovimentacaoEstoque.ESTORNO;
        movimentacao.quantidade = saida.getQuantidade();
        movimentacao.motivo = MotivoMovimentacaoEstoque.ESTORNO;
        movimentacao.lote = "ESTORNO-PEDIDO-" + pedido.getId();
        movimentacao.custoUnitario = saida.getCustoUnitario();
        movimentacao.pedido = pedido;
        movimentacao.usuario = usuario;
        return movimentacao;
    }

    public static EstoqueMovimentacao saidaManual(
            Ingrediente ingrediente,
            BigDecimal quantidade,
            MotivoMovimentacaoEstoque motivo,
            String lote,
            LocalDate validade,
            Usuario usuario) {
        EstoqueMovimentacao movimentacao = new EstoqueMovimentacao();
        movimentacao.ingrediente = ingrediente;
        movimentacao.tipo = TipoMovimentacaoEstoque.SAIDA;
        movimentacao.quantidade = quantidade;
        movimentacao.motivo = motivo;
        movimentacao.lote = lote;
        movimentacao.validade = validade;
        movimentacao.custoUnitario = ingrediente.getCustoUnitario();
        movimentacao.usuario = usuario;
        return movimentacao;
    }

    public static EstoqueMovimentacao entradaCompra(
            Ingrediente ingrediente,
            BigDecimal quantidade,
            BigDecimal custoUnitario,
            PedidoCompra pedidoCompra,
            String lote,
            LocalDate validade,
            Usuario usuario) {
        EstoqueMovimentacao movimentacao = new EstoqueMovimentacao();
        movimentacao.ingrediente = ingrediente;
        movimentacao.tipo = TipoMovimentacaoEstoque.ENTRADA;
        movimentacao.quantidade = quantidade;
        movimentacao.motivo = MotivoMovimentacaoEstoque.COMPRA;
        movimentacao.lote = lote;
        movimentacao.validade = validade;
        movimentacao.custoUnitario = custoUnitario;
        movimentacao.pedidoCompra = pedidoCompra;
        movimentacao.usuario = usuario;
        return movimentacao;
    }

    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }
    public TipoMovimentacaoEstoque getTipo() { return tipo; }
    public void setTipo(TipoMovimentacaoEstoque tipo) { this.tipo = tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public MotivoMovimentacaoEstoque getMotivo() { return motivo; }
    public void setMotivo(MotivoMovimentacaoEstoque motivo) { this.motivo = motivo; }
    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }
    public LocalDate getValidade() { return validade; }
    public void setValidade(LocalDate validade) { this.validade = validade; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public PedidoCompra getPedidoCompra() { return pedidoCompra; }
    public void setPedidoCompra(PedidoCompra pedidoCompra) { this.pedidoCompra = pedidoCompra; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
