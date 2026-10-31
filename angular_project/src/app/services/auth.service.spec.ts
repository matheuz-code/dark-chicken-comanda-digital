import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('Mudança de sessão autenticada', () => {
  afterEach(() => localStorage.clear());
  it('notifica consumidores após login e logout reais do serviço', () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(AuthService);
    const http = TestBed.inject(HttpTestingController);
    const observed: Array<number | null> = [];
    service.sessaoAlterada.subscribe(() => observed.push(service.usuario()?.id ?? null));
    service.login('cliente@example.invalid', 'test-only-password').subscribe();
    http
      .expectOne(`${environment.apiUrl}/auth/login`)
      .flush({
        token: 'test-only-token',
        tipo: 'Bearer',
        expiraEmSegundos: 600,
        usuario: {
          id: 1,
          nome: 'Cliente fictício',
          email: 'cliente@example.invalid',
          perfil: 'CLIENTE',
          status: 'ATIVO',
        },
      });
    service.logout();
    expect(observed).toEqual([1, null]);
    expect(service.estaAutenticado()).toBe(false);
    http.verify();
  });
});
