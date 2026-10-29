import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService, PerfilUsuario } from '../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  protected readonly mode = signal<'login' | 'signup'>('login');
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly title = computed(() =>
    this.mode() === 'login' ? 'Entrar' : 'Cadastrar'
  );

  protected readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
  });

  protected readonly signupForm = this.fb.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
      nome: ['', [Validators.required, Validators.maxLength(120)]],
      telefone: ['', [Validators.required, Validators.maxLength(30)]],
      endereco: ['', [Validators.required, Validators.maxLength(500)]],
    },
    { validators: [passwordsMatchValidator] }
  );

  protected setMode(next: 'login' | 'signup') {
    this.mode.set(next);
    this.errorMessage.set('');
  }

  protected onSubmit() {
    const form = this.mode() === 'login' ? this.loginForm : this.signupForm;
    form.markAllAsTouched();
    if (form.invalid) {
      this.errorMessage.set(
        form.hasError('passwordsMismatch')
          ? 'As senhas não coincidem.'
          : 'Preencha os campos obrigatórios, informe um e-mail válido e use uma senha de 6 a 72 caracteres.'
      );
      return;
    }

    if (this.mode() === 'login') {
      const { email, password } = this.loginForm.getRawValue();
      this.loading.set(true);
      this.errorMessage.set('');
      this.authService.login(email ?? '', password ?? '').subscribe({
        next: ({ usuario }) => {
          this.loading.set(false);
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          this.router.navigateByUrl(
            returnUrl?.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : this.rotaInicial(usuario.perfil)
          );
        },
        error: (error: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(
            error.status === 401
              ? 'E-mail ou senha inválidos.'
              : 'Não foi possível entrar. Verifique se o servidor está ativo.'
          );
        },
      });
      return;
    }

    const { nome, email, password, telefone, endereco } = this.signupForm.getRawValue();
    this.loading.set(true);
    this.errorMessage.set('');
    this.authService.cadastrarCliente({
      nome: (nome ?? '').trim(),
      email: (email ?? '').trim(),
      senha: password ?? '',
      telefone: (telefone ?? '').trim(),
      endereco: (endereco ?? '').trim(),
    }).subscribe({
      next: () => {
        this.loading.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        void this.router.navigateByUrl(
          returnUrl?.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/cardapio'
        );
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(
          error.status === 409
            ? 'Este e-mail já está cadastrado. Entre na sua conta.'
            : error.status === 400
              ? 'Confira os dados do cadastro e tente novamente.'
              : 'Não foi possível cadastrar. Tente novamente em instantes.'
        );
      },
    });
  }

  private rotaInicial(perfil: PerfilUsuario): string {
    if (perfil === 'ADMIN' || perfil === 'GERENTE') return '/admin';
    if (perfil === 'COZINHEIRO') return '/cozinha';
    return '/cardapio';
  }
}

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;

  if (!password || !confirm) return null;
  return password === confirm ? null : { passwordsMismatch: true };
}
