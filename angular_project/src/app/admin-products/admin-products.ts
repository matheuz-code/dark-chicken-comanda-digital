import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import {
  CategoriaAdmin,
  FichaTecnicaItemRequest,
  FichaTecnicaResponse,
  IngredienteAdmin,
  PratoAdmin,
  UnidadeMedida,
} from '../models/admin.model';
import { AdminApiService } from '../services/admin-api.service';

type AbaCatalogo = 'categorias' | 'pratos' | 'ingredientes' | 'fichas';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-products.html',
  styleUrl: './admin-products.css',
})
export class AdminProducts implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  protected aba: AbaCatalogo = 'categorias';
  protected categorias: CategoriaAdmin[] = [];
  protected pratos: PratoAdmin[] = [];
  protected ingredientes: IngredienteAdmin[] = [];
  protected fichaAtual?: FichaTecnicaResponse;
  protected fichaExistente = false;
  protected loading = true;
  protected saving = false;
  protected message = '';
  protected errorMessage = '';
  protected readonly unidades: UnidadeMedida[] = ['UN', 'G', 'KG', 'ML', 'L'];

  protected readonly categoriaForm = this.fb.nonNullable.group({
    id: [0],
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    descricao: ['', Validators.maxLength(500)],
    ordem: [0, [Validators.required, Validators.min(0)]],
    status: ['ATIVO' as 'ATIVO' | 'INATIVO', Validators.required],
  });

  protected readonly pratoForm = this.fb.nonNullable.group({
    id: [0],
    categoriaId: [0, [Validators.required, Validators.min(1)]],
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    descricao: ['', Validators.maxLength(1000)],
    fotoUrl: ['', Validators.maxLength(1000)],
    precoVenda: [0, [Validators.required, Validators.min(0)]],
    tempoPreparoMin: [1, [Validators.required, Validators.min(1)]],
    status: ['INATIVO' as 'ATIVO' | 'INATIVO', Validators.required],
  });

  protected readonly ingredienteForm = this.fb.nonNullable.group({
    id: [0],
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    sku: ['', [Validators.required, Validators.pattern('[A-Za-z0-9][A-Za-z0-9._-]*')]],
    unidadePadrao: ['UN' as UnidadeMedida, Validators.required],
    estoqueMinimo: [0, [Validators.required, Validators.min(0)]],
    custoUnitario: [0, [Validators.required, Validators.min(0)]],
    status: ['ATIVO' as 'ATIVO' | 'INATIVO', Validators.required],
  });

  protected readonly fichaForm = this.fb.nonNullable.group({
    pratoId: [0, [Validators.required, Validators.min(1)]],
    rendimento: [1, [Validators.required, Validators.min(1)]],
    modoPreparo: ['', [Validators.required, Validators.maxLength(10_000)]],
    itens: this.fb.array([this.criarItemFicha()]),
  });

  protected get itensFicha() {
    return this.fichaForm.controls.itens;
  }

  ngOnInit(): void {
    this.carregar();
  }

  protected voltar(): void {
    void this.router.navigateByUrl('/admin');
  }

  protected selecionarAba(aba: AbaCatalogo): void {
    this.aba = aba;
    this.limparMensagens();
  }

  protected carregar(): void {
    this.loading = true;
    this.limparMensagens();
    forkJoin({
      categorias: this.api.listarCategorias(),
      pratos: this.api.listarPratos(),
      ingredientes: this.api.listarIngredientes(),
    }).subscribe({
      next: ({ categorias, pratos, ingredientes }) => {
        this.categorias = categorias.conteudo;
        this.pratos = pratos.conteudo;
        this.ingredientes = ingredientes.conteudo;
        this.loading = false;
      },
      error: (error) => this.tratarErro(error, 'Não foi possível carregar o catálogo.'),
    });
  }

  protected salvarCategoria(): void {
    if (this.categoriaForm.invalid) return this.categoriaForm.markAllAsTouched();
    const { id, ...request } = this.categoriaForm.getRawValue();
    this.executarSalvamento(
      this.api.salvarCategoria(id || null, request),
      'Categoria salva com sucesso.',
      () => this.novaCategoria()
    );
  }

  protected editarCategoria(categoria: CategoriaAdmin): void {
    this.categoriaForm.setValue({
      id: categoria.id,
      nome: categoria.nome,
      descricao: categoria.descricao ?? '',
      ordem: categoria.ordem,
      status: categoria.status,
    });
  }

  protected novaCategoria(): void {
    this.categoriaForm.reset({ id: 0, nome: '', descricao: '', ordem: 0, status: 'ATIVO' });
  }

  protected desativarCategoria(id: number): void {
    this.executarExclusao(this.api.desativarCategoria(id), 'Categoria desativada.');
  }

  protected salvarPrato(): void {
    if (this.pratoForm.invalid) return this.pratoForm.markAllAsTouched();
    const { id, ...request } = this.pratoForm.getRawValue();
    this.executarSalvamento(
      this.api.salvarPrato(id || null, request),
      'Prato salvo com sucesso.',
      () => this.novoPrato()
    );
  }

  protected editarPrato(prato: PratoAdmin): void {
    this.pratoForm.setValue({
      id: prato.id,
      categoriaId: prato.categoria.id,
      nome: prato.nome,
      descricao: prato.descricao ?? '',
      fotoUrl: prato.fotoUrl ?? '',
      precoVenda: prato.precoVenda,
      tempoPreparoMin: prato.tempoPreparoMin,
      status: prato.status,
    });
  }

  protected novoPrato(): void {
    this.pratoForm.reset({
      id: 0,
      categoriaId: this.categorias[0]?.id ?? 0,
      nome: '',
      descricao: '',
      fotoUrl: '',
      precoVenda: 0,
      tempoPreparoMin: 1,
      status: 'INATIVO',
    });
  }

  protected desativarPrato(id: number): void {
    this.executarExclusao(this.api.desativarPrato(id), 'Prato desativado.');
  }

  protected salvarIngrediente(): void {
    if (this.ingredienteForm.invalid) return this.ingredienteForm.markAllAsTouched();
    const { id, ...request } = this.ingredienteForm.getRawValue();
    this.executarSalvamento(
      this.api.salvarIngrediente(id || null, request),
      'Ingrediente salvo com sucesso.',
      () => this.novoIngrediente()
    );
  }

  protected editarIngrediente(ingrediente: IngredienteAdmin): void {
    this.ingredienteForm.setValue({
      id: ingrediente.id,
      nome: ingrediente.nome,
      sku: ingrediente.sku,
      unidadePadrao: ingrediente.unidadePadrao,
      estoqueMinimo: ingrediente.estoqueMinimo,
      custoUnitario: ingrediente.custoUnitario,
      status: ingrediente.status,
    });
  }

  protected novoIngrediente(): void {
    this.ingredienteForm.reset({
      id: 0,
      nome: '',
      sku: '',
      unidadePadrao: 'UN',
      estoqueMinimo: 0,
      custoUnitario: 0,
      status: 'ATIVO',
    });
  }

  protected desativarIngrediente(id: number): void {
    this.executarExclusao(this.api.desativarIngrediente(id), 'Ingrediente desativado.');
  }

  protected selecionarFicha(pratoId: number): void {
    if (!pratoId) return;
    this.fichaForm.controls.pratoId.setValue(pratoId);
    this.fichaAtual = undefined;
    this.fichaExistente = false;
    this.api.buscarFicha(pratoId).subscribe({
      next: (ficha) => {
        this.fichaAtual = ficha;
        this.fichaExistente = true;
        this.preencherFicha(ficha);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 404) {
          this.fichaForm.patchValue({ rendimento: 1, modoPreparo: '' });
          this.itensFicha.clear();
          this.itensFicha.push(this.criarItemFicha());
          return;
        }
        this.tratarErro(error, 'Não foi possível consultar a ficha técnica.');
      },
    });
  }

  protected adicionarItemFicha(): void {
    this.itensFicha.push(this.criarItemFicha());
  }

  protected removerItemFicha(index: number): void {
    if (this.itensFicha.length > 1) this.itensFicha.removeAt(index);
  }

  protected salvarFicha(): void {
    if (this.fichaForm.invalid) return this.fichaForm.markAllAsTouched();
    const { pratoId, ...request } = this.fichaForm.getRawValue();
    this.saving = true;
    this.api.salvarFicha(pratoId, request, this.fichaExistente).subscribe({
      next: (ficha) => {
        this.fichaAtual = ficha;
        this.fichaExistente = true;
        this.saving = false;
        this.message = 'Ficha técnica e food cost atualizados.';
        this.carregarSilenciosamente();
      },
      error: (error) => this.tratarErro(error, 'Não foi possível salvar a ficha técnica.'),
    });
  }

  private criarItemFicha(item?: Partial<FichaTecnicaItemRequest>) {
    return this.fb.nonNullable.group({
      ingredienteId: [item?.ingredienteId ?? 0, [Validators.required, Validators.min(1)]],
      quantidade: [item?.quantidade ?? 0.0001, [Validators.required, Validators.min(0.0001)]],
      unidade: [item?.unidade ?? ('UN' as UnidadeMedida), Validators.required],
      fatorCorrecao: [item?.fatorCorrecao ?? 1, [Validators.required, Validators.min(1)]],
    });
  }

  private preencherFicha(ficha: FichaTecnicaResponse): void {
    this.fichaForm.patchValue({
      pratoId: ficha.pratoId,
      rendimento: ficha.rendimento,
      modoPreparo: ficha.modoPreparo,
    });
    this.itensFicha.clear();
    ficha.itens.forEach((item) => this.itensFicha.push(this.criarItemFicha({
      ingredienteId: item.ingrediente.id,
      quantidade: item.quantidade,
      unidade: item.unidade,
      fatorCorrecao: item.fatorCorrecao,
    })));
  }

  private executarSalvamento<T>(
    request: Observable<T>,
    mensagem: string,
    aoConcluir: () => void
  ): void {
    this.saving = true;
    this.limparMensagens();
    request.subscribe({
      next: () => {
        this.saving = false;
        this.message = mensagem;
        aoConcluir();
        this.carregarSilenciosamente();
      },
      error: (error: HttpErrorResponse) => this.tratarErro(error, 'Não foi possível salvar.'),
    });
  }

  private executarExclusao(request: Observable<void>, mensagem: string): void {
    request.subscribe({
      next: () => {
        this.message = mensagem;
        this.carregarSilenciosamente();
      },
      error: (error: HttpErrorResponse) => this.tratarErro(error, 'Não foi possível desativar.'),
    });
  }

  private carregarSilenciosamente(): void {
    forkJoin({
      categorias: this.api.listarCategorias(),
      pratos: this.api.listarPratos(),
      ingredientes: this.api.listarIngredientes(),
    }).subscribe(({ categorias, pratos, ingredientes }) => {
      this.categorias = categorias.conteudo;
      this.pratos = pratos.conteudo;
      this.ingredientes = ingredientes.conteudo;
    });
  }

  private tratarErro(error: HttpErrorResponse, fallback: string): void {
    this.loading = false;
    this.saving = false;
    this.errorMessage = error.error?.mensagem ?? fallback;
  }

  private limparMensagens(): void {
    this.message = '';
    this.errorMessage = '';
  }
}
