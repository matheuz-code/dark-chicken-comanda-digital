package br.edu.unasp.comandadigital.entity;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria")
public class Categoria extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private Integer ordem = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtivacao status = StatusAtivacao.ATIVO;

    protected Categoria() {
    }

    public static Categoria criar(
            String nome,
            String descricao,
            Integer ordem,
            StatusAtivacao status) {
        Categoria categoria = new Categoria();
        categoria.nome = nome;
        categoria.descricao = descricao;
        categoria.ordem = ordem;
        categoria.status = status;
        return categoria;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }
    public StatusAtivacao getStatus() { return status; }
    public void setStatus(StatusAtivacao status) { this.status = status; }
}
