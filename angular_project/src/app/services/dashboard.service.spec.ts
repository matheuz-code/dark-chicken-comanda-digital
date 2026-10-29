import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { jwtInterceptor } from '../interceptors/jwt.interceptor';
import { AuthService, AutenticacaoResponse } from './auth.service';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let authService: AuthService;
  let dashboardService: DashboardService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.removeItem('comanda-digital.auth');
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    authService = TestBed.inject(AuthService);
    dashboardService = TestBed.inject(DashboardService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.removeItem('comanda-digital.auth');
  });

  it('deve autenticar e carregar KPIs e Top 5 com o JWT', () => {
    authService.login('admin@email.com', 'senha123').subscribe();
    const loginRequest = httpTesting.expectOne(`${environment.apiUrl}/auth/login`);
    expect(loginRequest.request.method).toBe('POST');
    loginRequest.flush(sessaoAdmin());

    dashboardService.carregar(5).subscribe((dashboard) => {
      expect(dashboard.kpis.faturamento).toBe(14);
      expect(dashboard.topPratos[0].pratoNome).toBe('Refrigerante lata');
    });

    const kpisRequest = httpTesting.expectOne(`${environment.apiUrl}/admin/dashboard`);
    const topRequest = httpTesting.expectOne(
      (request) => request.url === `${environment.apiUrl}/admin/dashboard/top-pratos`
    );
    expect(kpisRequest.request.headers.get('Authorization')).toBe('Bearer jwt-dashboard');
    expect(topRequest.request.headers.get('Authorization')).toBe('Bearer jwt-dashboard');
    expect(topRequest.request.params.get('limit')).toBe('5');

    kpisRequest.flush({
      data: '2026-08-26',
      faturamento: 14,
      totalPedidos: 1,
      ticketMedio: 14,
      custoIngredientesEstimado: 8,
      foodCostMedioPercentual: 57.14,
    });
    topRequest.flush([
      {
        pratoId: 5,
        pratoNome: 'Refrigerante lata',
        quantidadeVendida: 2,
        faturamento: 14,
      },
    ]);
  });
});

function sessaoAdmin(): AutenticacaoResponse {
  return {
    token: 'jwt-dashboard',
    tipo: 'Bearer',
    expiraEmSegundos: 28800,
    usuario: {
      id: 1,
      nome: 'Administrador',
      email: 'admin@email.com',
      perfil: 'ADMIN',
      status: 'ATIVO',
    },
  };
}
