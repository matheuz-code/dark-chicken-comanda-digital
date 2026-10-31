import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { PaginaResponse, PratoCardapioResponse } from '../models/cardapio.model';
import { CardapioService } from './cardapio.service';

describe('CardapioService', () => {
  let service: CardapioService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CardapioService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('deve listar o cardápio com paginação e filtro por categoria', () => {
    const response = menuPage();

    service.listar({ categoriaId: 2, pagina: 1, tamanho: 12 }).subscribe((page) => {
      expect(page).toEqual(response);
    });

    const request = httpTesting.expectOne(
      (candidate) => candidate.url === `${environment.apiUrl}/cardapio`,
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('categoriaId')).toBe('2');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('12');
    request.flush(response);
  });

  it('deve buscar o detalhe público de um prato', () => {
    service.obterPorId(7).subscribe((dish) => {
      expect(dish.id).toBe(7);
      expect(dish.tempoPreparoMin).toBe(20);
    });

    const request = httpTesting.expectOne(`${environment.apiUrl}/cardapio/7`);
    expect(request.request.method).toBe('GET');
    request.flush({
      ...menuPage().conteudo[0],
      id: 7,
      tempoPreparoMin: 20,
    });
  });
});

function menuPage(): PaginaResponse<PratoCardapioResponse> {
  return {
    conteudo: [
      {
        id: 1,
        nome: 'X-Burger',
        descricao: 'Hambúrguer artesanal',
        fotoUrl: 'https://example.com/burger.jpg',
        precoVenda: 25.9,
        categoria: { id: 1, nome: 'Lanches' },
      },
    ],
    pagina: 0,
    tamanho: 12,
    totalElementos: 1,
    totalPaginas: 1,
    primeira: true,
    ultima: true,
  };
}

describe('Contrato real do cardápio', () => {
  let api: CardapioService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CardapioService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('recusa HTTP 200 incompatível sem repetir nem fingir cardápio vazio', async () => {
    const result = firstValueFrom(api.listarComRecuperacao());
    const rejected = expect(result).rejects.toThrow('incompatível');
    http.expectOne((req) => req.url.endsWith('/cardapio')).flush({ status: 'starting' });
    await rejected;
    http.expectNone((req) => req.url.endsWith('/cardapio'));
  });
  it('aceita catálogo vazio válido sem inventar pratos', async () => {
    const result = firstValueFrom(api.listarComRecuperacao());
    http.expectOne((req) => req.url.endsWith('/cardapio')).flush({ conteudo: [] });
    expect((await result).conteudo).toEqual([]);
  });
});
