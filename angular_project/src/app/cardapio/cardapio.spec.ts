import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PaginaResponse, PratoCardapioResponse } from '../models/cardapio.model';
import { OrderStateService } from '../order-state.service';
import { CardapioService, ListarCardapioParams } from '../services/cardapio.service';
import { Cardapio } from './cardapio';

registerLocaleData(localePt, 'pt-BR');

describe('Cardapio', () => {
  let fixture: ComponentFixture<Cardapio>;
  let cardapioService: {
    listar: ReturnType<typeof vi.fn>;
    obterPorId: ReturnType<typeof vi.fn>;
    listarComRecuperacao: ReturnType<typeof vi.fn>;
  };
  let orderState: { setItems: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    const dishes = [burger(), drink()];
    cardapioService = {
      listar: vi.fn((params: ListarCardapioParams = {}) =>
        of(
          menuPage(
            params.categoriaId
              ? dishes.filter((dish) => dish.categoria.id === params.categoriaId)
              : dishes,
          ),
        ),
      ),
      obterPorId: vi.fn(() => of({ ...burger(), tempoPreparoMin: 20 })),
      listarComRecuperacao: vi.fn((params: ListarCardapioParams = {}) =>
        of(
          menuPage(
            params.categoriaId
              ? dishes.filter((dish) => dish.categoria.id === params.categoriaId)
              : dishes,
          ),
        ),
      ),
    };
    orderState = { setItems: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Cardapio],
      providers: [
        provideRouter([]),
        { provide: CardapioService, useValue: cardapioService },
        { provide: OrderStateService, useValue: orderState },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Cardapio);
    fixture.detectChanges();
  });

  it('deve renderizar pratos e categorias retornados pela API', () => {
    const content = fixture.nativeElement as HTMLElement;

    expect(cardapioService.listarComRecuperacao).toHaveBeenCalledWith(
      {
        categoriaId: undefined,
        pagina: 0,
        tamanho: 100,
      },
      expect.any(Function),
    );
    expect(content.textContent).toContain('X-Burger');
    expect(content.textContent).toContain('Refrigerante');
    expect(findButton('Lanches')).toBeTruthy();
    expect(findButton('Bebidas')).toBeTruthy();
  });

  it('deve filtrar pela categoria na API e preservar o carrinho', () => {
    findButton('+ Adicionar').click();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Seu Carrinho (1 item)');

    findButton('Bebidas').click();
    fixture.detectChanges();

    expect(cardapioService.listarComRecuperacao).toHaveBeenLastCalledWith(
      {
        categoriaId: 2,
        pagina: 0,
        tamanho: 100,
      },
      expect.any(Function),
    );
    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(content).toContain('Refrigerante');
    expect(content).not.toContain('Hambúrguer artesanal');
    expect(content).toContain('Seu Carrinho (1 item)');
  });

  it('deve carregar o detalhe real do prato', () => {
    findButton('Detalhes').click();
    fixture.detectChanges();

    expect(cardapioService.obterPorId).toHaveBeenCalledWith(1);
    const dialog = (fixture.nativeElement as HTMLElement).querySelector('[role="dialog"]');
    expect(dialog?.textContent).toContain('Tempo estimado: 20 minutos');
    expect(dialog?.textContent).toContain('X-Burger');
  });

  it('deve pesquisar e tratar resultados vazios sem alterar o catálogo', () => {
    const content = fixture.nativeElement as HTMLElement;
    const input = content.querySelector('#menu-search') as HTMLInputElement;
    input.value = '  BURGER  ';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(content.querySelectorAll('.product-card')).toHaveLength(1);
    expect(content.querySelector('.product-card')?.textContent).toContain('X-Burger');
    input.value = 'produto inexistente';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(content.querySelectorAll('.product-card')).toHaveLength(0);
    expect(content.textContent).toContain('Nenhum prato encontrado');
  });

  function findButton(label: string): HTMLButtonElement {
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ) as HTMLButtonElement[];
    const button = buttons.find((candidate) => candidate.textContent?.trim() === label);
    if (!button) throw new Error(`Botão não encontrado: ${label}`);
    return button;
  }
});

function burger(): PratoCardapioResponse {
  return {
    id: 1,
    nome: 'X-Burger',
    descricao: 'Hambúrguer artesanal',
    fotoUrl: 'https://example.com/burger.jpg',
    precoVenda: 25.9,
    categoria: { id: 1, nome: 'Lanches' },
  };
}

function drink(): PratoCardapioResponse {
  return {
    id: 5,
    nome: 'Refrigerante',
    descricao: 'Bebida gelada',
    fotoUrl: null,
    precoVenda: 8,
    categoria: { id: 2, nome: 'Bebidas' },
  };
}

function menuPage(dishes: PratoCardapioResponse[]): PaginaResponse<PratoCardapioResponse> {
  return {
    conteudo: dishes,
    pagina: 0,
    tamanho: 100,
    totalElementos: dishes.length,
    totalPaginas: dishes.length ? 1 : 0,
    primeira: true,
    ultima: true,
  };
}
