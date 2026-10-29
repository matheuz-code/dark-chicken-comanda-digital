package br.edu.unasp.comandadigital.entity;

import br.edu.unasp.comandadigital.entity.enums.PerfilUsuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
public class Usuario extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 60)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil;

    @Column(length = 30)
    private String telefone;

    @Column(length = 500)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtivacao status = StatusAtivacao.ATIVO;

    protected Usuario() {
    }

    public static Usuario novoCliente(
            String nome,
            String email,
            String senhaHash,
            String telefone,
            String endereco) {
        Usuario usuario = new Usuario();
        usuario.nome = nome;
        usuario.email = email;
        usuario.senhaHash = senhaHash;
        usuario.perfil = PerfilUsuario.CLIENTE;
        usuario.telefone = telefone;
        usuario.endereco = endereco;
        usuario.status = StatusAtivacao.ATIVO;
        return usuario;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public PerfilUsuario getPerfil() { return perfil; }
    public void setPerfil(PerfilUsuario perfil) { this.perfil = perfil; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public StatusAtivacao getStatus() { return status; }
    public void setStatus(StatusAtivacao status) { this.status = status; }
}
