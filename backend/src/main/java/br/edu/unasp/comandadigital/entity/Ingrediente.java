package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ingrediente", uniqueConstraints = @UniqueConstraint(name = "uk_ingrediente_sku", columnNames = "sku"))
public class Ingrediente extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 50)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_padrao", nullable = false, length = 10)
    private UnidadeMedida unidadePadrao;

    @Column(name = "estoque_minimo", nullable = false, precision = 14, scale = 4)
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    @Column(name = "custo_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtivacao status = StatusAtivacao.ATIVO;

    protected Ingrediente() {
    }

    public static Ingrediente criar(
            String nome,
            String sku,
            UnidadeMedida unidadePadrao,
            BigDecimal estoqueMinimo,
            BigDecimal custoUnitario,
            StatusAtivacao status) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.nome = nome;
        ingrediente.sku = sku;
        ingrediente.unidadePadrao = unidadePadrao;
        ingrediente.estoqueMinimo = estoqueMinimo;
        ingrediente.custoUnitario = custoUnitario;
        ingrediente.status = status;
        return ingrediente;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public UnidadeMedida getUnidadePadrao() { return unidadePadrao; }
    public void setUnidadePadrao(UnidadeMedida unidadePadrao) { this.unidadePadrao = unidadePadrao; }
    public BigDecimal getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(BigDecimal estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal custoUnitario) { this.custoUnitario = custoUnitario; }
    public StatusAtivacao getStatus() { return status; }
    public void setStatus(StatusAtivacao status) { this.status = status; }
}
