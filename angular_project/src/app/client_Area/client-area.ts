import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioAdmin } from '../models/admin.model';
import { PerfilUsuario } from '../services/auth.service';
import { AdminApiService } from '../services/admin-api.service';

@Component({
  selector: 'app-admin-customers',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './client-area.html',
  styleUrl: './client-area.css',
})
export class AdminCustomers implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  protected usuarios: UsuarioAdmin[] = [];
  protected loading = true;
  protected saving = false;
  protected message = '';
  protected errorMessage = '';
  protected readonly perfis: PerfilUsuario[] = ['ADMIN', 'GERENTE', 'COZINHEIRO', 'CLIENTE'];

  protected readonly form = this.fb.nonNullable.group({
    id: [0],
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.minLength(6)],
    perfil: ['CLIENTE' as PerfilUsuario, Validators.required],
    telefone: [''],
    endereco: [''],
    status: ['ATIVO' as 'ATIVO' | 'INATIVO', Validators.required],
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected voltar(): void {
    void this.router.navigateByUrl('/admin');
  }

  protected carregar(): void {
    this.loading = true;
    this.api.listarUsuarios().subscribe({
      next: (pagina) => {
        this.usuarios = pagina.conteudo;
        this.loading = false;
      },
      error: (error) => this.tratarErro(error, 'Não foi possível carregar os usuários.'),
    });
  }

  protected salvar(): void {
    const raw = this.form.getRawValue();
    if (!raw.id && raw.senha.length < 6) {
      this.errorMessage = 'Informe uma senha com pelo menos 6 caracteres para o novo usuário.';
      return;
    }
    if (this.form.invalid) return this.form.markAllAsTouched();
    this.saving = true;
    this.errorMessage = '';
    const { id, ...request } = raw;
    const operacao = id
      ? this.api.atualizarUsuario(id, { ...request, senha: request.senha || undefined })
      : this.api.criarUsuario(request);
    operacao.subscribe({
      next: () => {
        this.message = 'Usuário salvo com sucesso.';
        this.novo();
        this.saving = false;
        this.carregar();
      },
      error: (error) => this.tratarErro(error, 'Não foi possível salvar o usuário.'),
    });
  }

  protected editar(usuario: UsuarioAdmin): void {
    this.form.setValue({
      id: usuario.id,
      nome: usuario.nome,
      email: usuario.email,
      senha: '',
      perfil: usuario.perfil,
      telefone: usuario.telefone ?? '',
      endereco: usuario.endereco ?? '',
      status: usuario.status,
    });
  }

  protected novo(): void {
    this.form.reset({
      id: 0, nome: '', email: '', senha: '', perfil: 'CLIENTE',
      telefone: '', endereco: '', status: 'ATIVO',
    });
  }

  protected desativar(id: number): void {
    this.api.desativarUsuario(id).subscribe({
      next: () => {
        this.message = 'Usuário desativado.';
        this.carregar();
      },
      error: (error) => this.tratarErro(error, 'Não foi possível desativar o usuário.'),
    });
  }

  private tratarErro(error: HttpErrorResponse, fallback: string): void {
    this.loading = false;
    this.saving = false;
    this.errorMessage = error.error?.mensagem ?? fallback;
  }
}
