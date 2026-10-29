package br.edu.unasp.comandadigital.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ficha_tecnica", uniqueConstraints = @UniqueConstraint(name = "uk_ficha_tecnica_prato", columnNames = "prato_id"))
public class FichaTecnica extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prato_id", nullable = false)
    private Prato prato;

    @Column(nullable = false)
    private Integer rendimento;

    @Column(name = "modo_preparo", nullable = false, columnDefinition = "TEXT")
    private String modoPreparo;

    protected FichaTecnica() {
    }

    public static FichaTecnica criar(
            Prato prato,
            Integer rendimento,
            String modoPreparo) {
        FichaTecnica ficha = new FichaTecnica();
        ficha.prato = prato;
        ficha.rendimento = rendimento;
        ficha.modoPreparo = modoPreparo;
        return ficha;
    }

    public Prato getPrato() { return prato; }
    public void setPrato(Prato prato) { this.prato = prato; }
    public Integer getRendimento() { return rendimento; }
    public void setRendimento(Integer rendimento) { this.rendimento = rendimento; }
    public String getModoPreparo() { return modoPreparo; }
    public void setModoPreparo(String modoPreparo) { this.modoPreparo = modoPreparo; }
}
