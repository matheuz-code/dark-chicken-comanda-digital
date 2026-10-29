import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import {
  Fornecedor,
  IngredienteAdmin,
  MovimentacaoEstoque,
  PedidoCompra,
  SaldoEstoque,
  UnidadeMedida,
} from '../models/admin.model';
import { AdminApiService } from '../services/admin-api.service';

type AbaEstoque = 'saldos' | 'movimentacoes' | 'fornecedores' | 'compras';

@Component({
  selector: 'app-admin-inventory',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-inventory.html',
  styleUrl: './admin-inventory.css',
})
export class AdminInventory implements OnInit {
  private readonly api = inject(AdminApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  protected aba: AbaEstoque = 'saldos';
  protected saldos: SaldoEstoque[] = [];
  protected alertas: SaldoEstoque[] = [];
  protected movimentacoes: MovimentacaoEstoque[] = [];
  protected fornecedores: Fornecedor[] = [];
  protected compras: PedidoCompra[] = [];
  protected ingredientes: IngredienteAdmin[] = [];
  protected loading = true;
  protected saving = false;
  protected message = '';
  protected errorMessage = '';
  protected readonly unidades: UnidadeMedida[] = ['UN', 'G', 'KG', 'ML', 'L'];
  protected readonly motivos = ['PERDA', 'VENCIMENTO', 'QUEBRA', 'USO_INTERNO', 'AJUSTE'];

  protected readonly movimentacaoForm = this.fb.nonNullable.group({
    ingredienteId: [0, [Validators.required, Validators.min(1)]],
    quantidade: [0.0001, [Validators.required, Validators.min(0.0001)]],
    motivo: ['PERDA', Validators.required],
    lote: [''],
  });

  protected readonly fornecedorForm = this.fb.nonNullable.group({
    id: [0],
    razaoSocial: ['', Validators.required],
    cnpj: ['', Validators.required],
    telefone: [''],
    email: ['', Validators.email],
    categoriasProdutos: [''],
    status: ['ATIVO' as 'ATIVO' | 'INATIVO', Validators.required],
  });

  protected readonly catalogoForm = this.fb.nonNullable.group({
    fornecedorId: [0, [Validators.required, Validators.min(1)]],
    ingredienteId: [0, [Validators.required, Validators.min(1)]],
    preco: [0.0001, [Validators.required, Validators.min(0.0001)]],
    unidadeVenda: ['UN' as UnidadeMedida, Validators.required],
  });

  protected readonly compraForm = this.fb.nonNullable.group({
    id: [0],
    fornecedorId: [0, [Validators.required, Validators.min(1)]],
    itens: this.fb.array([this.criarItemCompra()]),
  });

  protected get itensCompra() {
    return this.compraForm.controls.itens;
  }

  ngOnInit(): void {
    this.carregar();
  }

  protected voltar(): void {
    void this.router.navigateByUrl('/admin');
  }

  protected selecionarAba(aba: AbaEstoque): void {
    this.aba = aba;
    this.limparMensagens();
  }

  protected carregar(): void {
    this.loading = true;
    this.limparMensagens();
    forkJoin({
      saldos: this.api.listarSaldos(),
      alertas: this.api.listarAlertas(),
      movimentacoes: this.api.listarMovimentacoes(),
      fornecedores: this.api.listarFornecedores(),
      compras: this.api.listarCompras(),
      ingredientes: this.api.listarIngredientes(),
    }).subscribe({
      next: (data) => {
        this.saldos = data.saldos.conteudo;
        this.alertas = data.alertas.conteudo;
        this.movimentacoes = data.movimentacoes.conteudo;
        this.fornecedores = data.fornecedores.conteudo;
        this.compras = data.compras.conteudo;
        this.ingredientes = data.ingredientes.conteudo;
        this.loading = false;
      },
      error: (error) => this.tratarErro(error, 'Não foi possível carregar estoque e compras.'),
    });
  }

  protected registrarSaida(): void {
    if (this.movimentacaoForm.invalid) return this.movimentacaoForm.markAllAsTouched();
    this.saving = true;
    this.api.registrarSaida(this.movimentacaoForm.getRawValue()).subscribe({
      next: () => {
        this.message = 'Saída registrada e saldo atualizado.';
        this.movimentacaoForm.patchValue({ quantidade: 0.0001, lote: '' });
        this.recarregar();
      },
      error: (error) => this.tratarErro(error, 'Não foi possível registrar a saída.'),
    });
  }

  protected salvarFornecedor(): void {
    if (this.fornecedorForm.invalid) return this.fornecedorForm.markAllAsTouched();
    const { id, ...request } = this.fornecedorForm.getRawValue();
    this.saving = true;
    this.api.salvarFornecedor(id || null, request).subscribe({
      next: () => {
        this.message = 'Fornecedor salvo com sucesso.';
        this.novoFornecedor();
        this.recarregar();
      },
      error: (error) => this.tratarErro(error, 'Não foi possível salvar o fornecedor.'),
    });
  }

  protected editarFornecedor(fornecedor: Fornecedor): void {
    this.fornecedorForm.setValue({
      id: fornecedor.id,
      razaoSocial: fornecedor.razaoSocial,
      cnpj: fornecedor.cnpj,
      telefone: fornecedor.telefone ?? '',
      email: fornecedor.email ?? '',
      categoriasProdutos: fornecedor.categoriasProdutos ?? '',
      status: fornecedor.status,
    });
  }

  protected novoFornecedor(): void {
    this.fornecedorForm.reset({
      id: 0, razaoSocial: '', cnpj: '', telefone: '', email: '',
      categoriasProdutos: '', status: 'ATIVO',
    });
  }

  protected desativarFornecedor(id: number): void {
    this.api.desativarFornecedor(id).subscribe({
      next: () => {
        this.message = 'Fornecedor desativado.';
        this.recarregar();
      },
      error: (error) => this.tratarErro(error, 'Não foi possível desativar o fornecedor.'),
    });
  }

  protected adicionarProdutoFornecedor(): void {
    if (this.catalogoForm.invalid) return this.catalogoForm.markAllAsTouched();
    const { fornecedorId, ...request } = this.catalogoForm.getRawValue();
    this.saving = true;
    this.api.adicionarProdutoFornecedor(fornecedorId, request).subscribe({
      next: () => {
        this.saving = false;
        this.message = 'Ingrediente adicionado ao catálogo do fornecedor.';
      },
      error: (error) => this.tratarErro(error, 'Não foi possível atualizar o catálogo.'),
    });
  }

  protected adicionarItemCompra(): void {
    this.itensCompra.push(this.criarItemCompra());
  }

  protected removerItemCompra(index: number): void {
    if (this.itensCompra.length > 1) this.itensCompra.removeAt(index);
  }

  protected salvarCompra(): void {
    if (this.compraForm.invalid) return this.compraForm.markAllAsTouched();
    const { id, ...request } = this.compraForm.getRawValue();
    this.saving = true;
    this.api.salvarCompra(id || null, request).subscribe({
      next: () => {
        this.message = 'Pedido de compra salvo como rascunho.';
        this.novaCompra();
        this.recarregar();
      },
      error: (error) => this.tratarErro(error, 'Não foi possível salvar a compra.'),
    });
  }

  protected editarCompra(compra: PedidoCompra): void {
    this.api.detalharCompra(compra.id).subscribe({
      next: (detalhe) => {
        this.compraForm.patchValue({ id: detalhe.id, fornecedorId: detalhe.fornecedorId });
        this.itensCompra.clear();
        detalhe.itens.forEach((item) => this.itensCompra.push(this.criarItemCompra(item)));
      },
      error: (error) => this.tratarErro(error, 'Não foi possível detalhar a compra.'),
    });
  }

  protected novaCompra(): void {
    this.compraForm.patchValue({ id: 0, fornecedorId: 0 });
    this.itensCompra.clear();
    this.itensCompra.push(this.criarItemCompra());
  }

  protected enviarCompra(id: number): void {
    this.executarCompra(this.api.enviarCompra(id), 'Pedido enviado ao fornecedor.');
  }

  protected receberCompra(id: number): void {
    this.executarCompra(this.api.receberCompra(id), 'Compra recebida e estoque atualizado.');
  }

  protected cancelarCompra(id: number): void {
    this.api.cancelarCompra(id).subscribe({
      next: () => {
        this.message = 'Compra cancelada.';
        this.recarregar();
      },
      error: (error) => this.tratarErro(error, 'Não foi possível cancelar a compra.'),
    });
  }

  private criarItemCompra(item?: { ingredienteId: number; quantidade: number; precoUnitario: number }) {
    return this.fb.nonNullable.group({
      ingredienteId: [item?.ingredienteId ?? 0, [Validators.required, Validators.min(1)]],
      quantidade: [item?.quantidade ?? 1, [Validators.required, Validators.min(0.0001)]],
      precoUnitario: [item?.precoUnitario ?? 0.0001, [Validators.required, Validators.min(0.0001)]],
    });
  }

  private executarCompra(request: Observable<unknown>, mensagem: string): void {
    this.saving = true;
    request.subscribe({
      next: () => {
        this.message = mensagem;
        this.recarregar();
      },
      error: (error: HttpErrorResponse) => this.tratarErro(error, 'Operação de compra não concluída.'),
    });
  }

  private recarregar(): void {
    this.saving = false;
    forkJoin({
      saldos: this.api.listarSaldos(),
      alertas: this.api.listarAlertas(),
      movimentacoes: this.api.listarMovimentacoes(),
      fornecedores: this.api.listarFornecedores(),
      compras: this.api.listarCompras(),
    }).subscribe((data) => {
      this.saldos = data.saldos.conteudo;
      this.alertas = data.alertas.conteudo;
      this.movimentacoes = data.movimentacoes.conteudo;
      this.fornecedores = data.fornecedores.conteudo;
      this.compras = data.compras.conteudo;
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
