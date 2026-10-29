package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.entity.enums.UnidadeMedida;
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
@Table(name = "ficha_tecnica_item", uniqueConstraints = @UniqueConstraint(
        name = "uk_ficha_item_ingrediente", columnNames = {"ficha_tecnica_id", "ingrediente_id"}))
public class FichaTecnicaItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_tecnica_id", nullable = false)
    private FichaTecnica fichaTecnica;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UnidadeMedida unidade;

    @Column(name = "fator_correcao", nullable = false, precision = 10, scale = 4)
    private BigDecimal fatorCorrecao = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtivacao status = StatusAtivacao.ATIVO;

    protected FichaTecnicaItem() {
    }

    public static FichaTecnicaItem criar(
            FichaTecnica fichaTecnica,
            Ingrediente ingrediente,
            BigDecimal quantidade,
            UnidadeMedida unidade,
            BigDecimal fatorCorrecao) {
        FichaTecnicaItem item = new FichaTecnicaItem();
        item.fichaTecnica = fichaTecnica;
        item.ingrediente = ingrediente;
        item.quantidade = quantidade;
        item.unidade = unidade;
        item.fatorCorrecao = fatorCorrecao;
        item.status = StatusAtivacao.ATIVO;
        return item;
    }

    public FichaTecnica getFichaTecnica() { return fichaTecnica; }
    public void setFichaTecnica(FichaTecnica fichaTecnica) { this.fichaTecnica = fichaTecnica; }
    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public UnidadeMedida getUnidade() { return unidade; }
    public void setUnidade(UnidadeMedida unidade) { this.unidade = unidade; }
    public BigDecimal getFatorCorrecao() { return fatorCorrecao; }
    public void setFatorCorrecao(BigDecimal fatorCorrecao) { this.fatorCorrecao = fatorCorrecao; }
    public StatusAtivacao getStatus() { return status; }
    public void setStatus(StatusAtivacao status) { this.status = status; }
}
