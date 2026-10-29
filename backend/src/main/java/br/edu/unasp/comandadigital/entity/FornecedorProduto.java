package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "fornecedor_produto", uniqueConstraints = @UniqueConstraint(
        name = "uk_fornecedor_produto", columnNames = {"fornecedor_id", "ingrediente_id"}))
public class FornecedorProduto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_venda", nullable = false, length = 10)
    private UnidadeMedida unidadeVenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtivacao status = StatusAtivacao.ATIVO;

    protected FornecedorProduto() {
    }

    public static FornecedorProduto criar(
            Fornecedor fornecedor,
            Ingrediente ingrediente,
            BigDecimal preco,
            UnidadeMedida unidadeVenda) {
        FornecedorProduto produto = new FornecedorProduto();
        produto.fornecedor = fornecedor;
        produto.ingrediente = ingrediente;
        produto.preco = preco;
        produto.unidadeVenda = unidadeVenda;
        produto.status = StatusAtivacao.ATIVO;
        return produto;
    }

    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor fornecedor) { this.fornecedor = fornecedor; }
    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public UnidadeMedida getUnidadeVenda() { return unidadeVenda; }
    public void setUnidadeVenda(UnidadeMedida unidadeVenda) { this.unidadeVenda = unidadeVenda; }
    public StatusAtivacao getStatus() { return status; }
    public void setStatus(StatusAtivacao status) { this.status = status; }
}
