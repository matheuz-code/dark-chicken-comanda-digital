package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "fornecedor_produto_preco_historico")
public class FornecedorProdutoPrecoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_produto_id", nullable = false)
    private FornecedorProduto fornecedorProduto;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal preco;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "unidade_venda", nullable = false, length = 10)
    private UnidadeMedida unidadeVenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FornecedorProdutoPrecoHistorico() {
    }

    public static FornecedorProdutoPrecoHistorico registrar(
            FornecedorProduto fornecedorProduto,
            BigDecimal preco,
            UnidadeMedida unidadeVenda,
            Usuario usuario) {
        FornecedorProdutoPrecoHistorico historico = new FornecedorProdutoPrecoHistorico();
        historico.fornecedorProduto = fornecedorProduto;
        historico.preco = preco;
        historico.unidadeVenda = unidadeVenda;
        historico.usuario = usuario;
        return historico;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public FornecedorProduto getFornecedorProduto() { return fornecedorProduto; }
    public BigDecimal getPreco() { return preco; }
    public UnidadeMedida getUnidadeVenda() { return unidadeVenda; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
