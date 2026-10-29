package br.edu.unasp.comandadigital.entity;

import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "fornecedor", uniqueConstraints = @UniqueConstraint(name = "uk_fornecedor_cnpj", columnNames = "cnpj"))
public class Fornecedor extends BaseEntity {

    @Column(name = "razao_social", nullable = false, length = 180)
    private String razaoSocial;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(length = 30)
    private String telefone;

    @Column(length = 180)
    private String email;

    @Column(name = "categorias_produtos", length = 500)
    private String categoriasProdutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtivacao status = StatusAtivacao.ATIVO;

    protected Fornecedor() {
    }

    public static Fornecedor criar(
            String razaoSocial,
            String cnpj,
            String telefone,
            String email,
            String categoriasProdutos,
            StatusAtivacao status) {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.razaoSocial = razaoSocial;
        fornecedor.cnpj = cnpj;
        fornecedor.telefone = telefone;
        fornecedor.email = email;
        fornecedor.categoriasProdutos = categoriasProdutos;
        fornecedor.status = status;
        return fornecedor;
    }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCategoriasProdutos() { return categoriasProdutos; }
    public void setCategoriasProdutos(String categoriasProdutos) { this.categoriasProdutos = categoriasProdutos; }
    public StatusAtivacao getStatus() { return status; }
    public void setStatus(StatusAtivacao status) { this.status = status; }
}
