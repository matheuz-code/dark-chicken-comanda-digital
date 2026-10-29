package br.edu.unasp.comandadigital.entity;

import java.math.BigDecimal;

import br.edu.unasp.comandadigital.entity.enums.StatusPrato;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "prato")
public class Prato extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(name = "foto_url", length = 1000)
    private String fotoUrl;

    @Column(name = "preco_venda", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "tempo_preparo_min", nullable = false)
    private Integer tempoPreparoMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPrato status = StatusPrato.INATIVO;

    protected Prato() {
    }

    public static Prato criar(
            Categoria categoria,
            String nome,
            String descricao,
            String fotoUrl,
            BigDecimal precoVenda,
            Integer tempoPreparoMin,
            StatusPrato status) {
        Prato prato = new Prato();
        prato.categoria = categoria;
        prato.nome = nome;
        prato.descricao = descricao;
        prato.fotoUrl = fotoUrl;
        prato.precoVenda = precoVenda;
        prato.tempoPreparoMin = tempoPreparoMin;
        prato.status = status;
        return prato;
    }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(BigDecimal precoVenda) { this.precoVenda = precoVenda; }
    public Integer getTempoPreparoMin() { return tempoPreparoMin; }
    public void setTempoPreparoMin(Integer tempoPreparoMin) { this.tempoPreparoMin = tempoPreparoMin; }
    public StatusPrato getStatus() { return status; }
    public void setStatus(StatusPrato status) { this.status = status; }
}
