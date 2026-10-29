import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { routes } from '../app.routes';
import type { PerfilUsuario } from '../services/auth.service';
import { authGuard } from './auth.guard';
import { roleGuard } from './role.guard';

describe('Guards de autenticação e perfil', () => {
  let router: Router;

  beforeEach(() => {
    localStorage.removeItem('comanda-digital.auth');
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient()],
    });
    router = TestBed.inject(Router);
  });

  afterEach(() => localStorage.removeItem('comanda-digital.auth'));

  it('deve enviar visitante para o login preservando a URL desejada', () => {
    const result = executarAuthGuard('/admin');

    expect(router.serializeUrl(result as UrlTree)).toBe(
      '/login?returnUrl=%2Fadmin'
    );
  });

  it('deve apagar sessão expirada e bloquear a rota protegida', () => {
    salvarSessao('ADMIN', Date.now() - 1);

    const result = executarAuthGuard('/admin');

    expect(router.serializeUrl(result as UrlTree)).toBe(
      '/login?returnUrl=%2Fadmin'
    );
    expect(localStorage.getItem('comanda-digital.auth')).toBeNull();
  });

  it('deve permitir um administrador em rota administrativa', () => {
    salvarSessao('ADMIN', Date.now() + 60_000);

    expect(executarRoleGuard('/admin', ['ADMIN', 'GERENTE'])).toBe(true);
  });

  it('deve enviar perfil sem permissão para acesso negado', () => {
    salvarSessao('CLIENTE', Date.now() + 60_000);

    const result = executarRoleGuard('/admin', ['ADMIN', 'GERENTE']);

    expect(router.serializeUrl(result as UrlTree)).toBe(
      '/acesso-negado?returnUrl=%2Fadmin'
    );
  });

  it('deve aplicar a matriz de rotas do SRS aos quatro perfis', () => {
    const matriz: Record<string, PerfilUsuario[]> = {
      admin: ['ADMIN', 'GERENTE'],
      'admin/estoque': ['ADMIN', 'GERENTE'],
      'admin/pedidos': ['ADMIN', 'GERENTE'],
      'admin/produtos': ['ADMIN'],
      'admin/clientes': ['ADMIN'],
      'finalizar-pedido': ['CLIENTE'],
      'cliente/perfil': ['CLIENTE'],
      motoboy: ['ADMIN'],
      cozinha: ['ADMIN', 'COZINHEIRO'],
      'acompanhar-pedido': ['CLIENTE'],
      'acompanhar-pedido/:id': ['CLIENTE'],
    };
    const perfis: PerfilUsuario[] = [
      'ADMIN',
      'GERENTE',
      'COZINHEIRO',
      'CLIENTE',
    ];

    for (const [path, permitidos] of Object.entries(matriz)) {
      const rota = routes.find((candidate) => candidate.path === path);
      expect(rota).toBeDefined();
      expect(rota?.data?.['roles']).toEqual(permitidos);

      for (const perfil of perfis) {
        salvarSessao(perfil, Date.now() + 60_000);
        const result = executarRoleGuard(`/${path}`, permitidos);

        if (permitidos.includes(perfil)) {
          expect(result).toBe(true);
        } else {
          expect(router.serializeUrl(result as UrlTree)).toBe(
            router.serializeUrl(
              router.createUrlTree(['/acesso-negado'], {
                queryParams: { returnUrl: `/${path}` },
              })
            )
          );
        }
      }
    }
  });
});

function executarAuthGuard(url: string) {
  return TestBed.runInInjectionContext(() =>
    authGuard(
      {} as ActivatedRouteSnapshot,
      { url } as RouterStateSnapshot
    )
  );
}

function executarRoleGuard(url: string, roles: PerfilUsuario[]) {
  return TestBed.runInInjectionContext(() =>
    roleGuard(
      { data: { roles } } as unknown as ActivatedRouteSnapshot,
      { url } as RouterStateSnapshot
    )
  );
}

function salvarSessao(perfil: PerfilUsuario, expiraEmEpochMs: number): void {
  localStorage.setItem(
    'comanda-digital.auth',
    JSON.stringify({
      token: 'jwt-teste',
      tipo: 'Bearer',
      expiraEmSegundos: 60,
      expiraEmEpochMs,
      usuario: {
        id: 1,
        nome: 'Usuário de teste',
        email: 'teste@email.com',
        perfil,
        status: 'ATIVO',
      },
    })
  );
}
