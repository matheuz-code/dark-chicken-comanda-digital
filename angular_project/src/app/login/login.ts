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
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  protected readonly signupForm = this.fb.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]],
      role: ['client', [Validators.required]],
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
    if (form.invalid) return;

    if (this.mode() === 'login') {
      const { email, password } = this.loginForm.getRawValue();
      this.loading.set(true);
      this.errorMessage.set('');
      this.authService.login(email ?? '', password ?? '').subscribe({
        next: ({ usuario }) => {
          this.loading.set(false);
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          this.router.navigateByUrl(
            returnUrl?.startsWith('/') ? returnUrl : this.rotaInicial(usuario.perfil)
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

    const role = this.signupForm.getRawValue().role;
    if (role === 'client') {
      this.router.navigateByUrl('/cardapio');
      return;
    }
    if (role === 'delivery') {
      this.router.navigateByUrl('/motoboy');
      return;
    }

    // Perfis ainda nao implementados no frontend atual.
    this.router.navigateByUrl('/');
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
