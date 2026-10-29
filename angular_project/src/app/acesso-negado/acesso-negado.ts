import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, PerfilUsuario } from '../services/auth.service';

@Component({
  selector: 'app-acesso-negado',
  template: `
    <main class="access-denied">
      <section class="access-denied__card" aria-labelledby="access-title">
        <span class="access-denied__code">403</span>
        <h1 id="access-title">Acesso não permitido</h1>
        <p>Seu perfil não possui permissão para acessar esta área.</p>
        <div class="access-denied__actions">
          <button type="button" class="primary" (click)="irParaAreaInicial()">
            Ir para minha área
          </button>
          <button type="button" class="secondary" (click)="irParaInicio()">
            Voltar ao início
          </button>
        </div>
      </section>
    </main>
  `,
  styles: `
    :host {
      display: block;
      min-height: 100vh;
      background: #f7f4ee;
    }

    .access-denied {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 2rem;
    }

    .access-denied__card {
      width: min(100%, 34rem);
      padding: 3rem 2rem;
      border-radius: 1.25rem;
      background: #fff;
      box-shadow: 0 1rem 3rem rgb(70 48 29 / 12%);
      text-align: center;
    }

    .access-denied__code {
      color: #ad2e24;
      font-size: 1rem;
      font-weight: 800;
      letter-spacing: 0.18em;
    }

    h1 {
      margin: 0.5rem 0 0.75rem;
      color: #2f241f;
      font-size: clamp(1.75rem, 5vw, 2.5rem);
    }

    p {
      margin: 0;
      color: #6d625c;
      line-height: 1.6;
    }

    .access-denied__actions {
      display: flex;
      justify-content: center;
      gap: 0.75rem;
      margin-top: 2rem;
      flex-wrap: wrap;
    }

    button {
      min-height: 2.75rem;
      padding: 0.7rem 1.1rem;
      border-radius: 0.7rem;
      border: 1px solid #ad2e24;
      font: inherit;
      font-weight: 700;
      cursor: pointer;
    }

    .primary {
      color: #fff;
      background: #ad2e24;
    }

    .secondary {
      color: #ad2e24;
      background: #fff;
    }
  `,
})
export class AcessoNegado {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected irParaAreaInicial(): void {
    void this.router.navigateByUrl(this.rotaInicial(this.authService.usuario()?.perfil));
  }

  protected irParaInicio(): void {
    void this.router.navigateByUrl('/');
  }

  private rotaInicial(perfil?: PerfilUsuario): string {
    if (perfil === 'ADMIN' || perfil === 'GERENTE') return '/admin';
    if (perfil === 'COZINHEIRO') return '/cozinha';
    return '/cardapio';
  }
}
