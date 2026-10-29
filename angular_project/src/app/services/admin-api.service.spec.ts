import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { forkJoin } from 'rxjs';
import { environment } from '../../environments/environment';
import { jwtInterceptor } from '../interceptors/jwt.interceptor';
import { AdminApiService } from './admin-api.service';

describe('AdminApiService', () => {
  let api: AdminApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.setItem(
      'comanda-digital.auth',
      JSON.stringify({
        token: 'jwt-admin-api',
        tipo: 'Bearer',
        expiraEmSegundos: 28800,
        expiraEmEpochMs: Date.now() + 60_000,
        usuario: {
          id: 1,
          nome: 'Administrador',
          email: 'admin@email.com',
          perfil: 'ADMIN',
          status: 'ATIVO',
        },
      })
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    api = TestBed.inject(AdminApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.removeItem('comanda-digital.auth');
  });

  it('deve consultar todas as áreas administrativas reais com JWT', () => {
    forkJoin([
      api.listarCategorias(),
      api.listarPratos(),
      api.listarIngredientes(),
      api.listarSaldos(),
      api.listarPedidos(),
      api.listarFornecedores(),
      api.listarCompras(),
      api.listarUsuarios(),
    ]).subscribe();

    const paths = [
      '/categorias', '/pratos', '/ingredientes', '/estoque/saldo',
      '/pedidos', '/fornecedores', '/compras', '/usuarios',
    ];
    for (const path of paths) {
      const request = httpTesting.expectOne(
        (candidate) => candidate.url === `${environment.apiUrl}/admin${path}`
      );
      expect(request.request.method).toBe('GET');
      expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-admin-api');
      expect(request.request.params.get('size')).toBe('100');
      request.flush(paginaVazia());
    }
  });

  it('deve usar os métodos e contratos corretos no fluxo de escrita', () => {
    api.salvarCategoria(null, {
      nome: 'Teste', descricao: '', ordem: 1, status: 'ATIVO',
    }).subscribe();
    const categoria = httpTesting.expectOne(`${environment.apiUrl}/admin/categorias`);
    expect(categoria.request.method).toBe('POST');
    categoria.flush({ id: 10, ...categoria.request.body });

    api.salvarFicha(10, {
      rendimento: 1,
      modoPreparo: 'Preparar',
      itens: [{ ingredienteId: 1, quantidade: 1, unidade: 'UN', fatorCorrecao: 1 }],
    }, false).subscribe();
    const ficha = httpTesting.expectOne(`${environment.apiUrl}/admin/pratos/10/ficha`);
    expect(ficha.request.method).toBe('POST');
    ficha.flush({ id: 1, pratoId: 10, nomePrato: 'Teste', custoUnitario: 1, foodCostPercentual: 10, classificacao: 'SAUDAVEL', ...ficha.request.body });

    api.atualizarStatusPedido(20, 'CONFIRMADO').subscribe();
    const pedido = httpTesting.expectOne(`${environment.apiUrl}/admin/pedidos/20/status`);
    expect(pedido.request.method).toBe('PATCH');
    expect(pedido.request.body.status).toBe('CONFIRMADO');
    pedido.flush({ id: 20, status: 'CONFIRMADO' });
  });
});

function paginaVazia() {
  return {
    conteudo: [], pagina: 0, tamanho: 100, totalElementos: 0,
    totalPaginas: 0, primeira: true, ultima: true,
  };
}
