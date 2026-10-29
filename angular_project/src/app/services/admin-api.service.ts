import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import {
  CategoriaAdmin,
  CategoriaAdminRequest,
  FichaTecnicaRequest,
  FichaTecnicaResponse,
  Fornecedor,
  FornecedorRequest,
  IngredienteAdmin,
  IngredienteAdminRequest,
  MovimentacaoEstoque,
  PaginaResponse,
  PedidoAdmin,
  PedidoAdminDetalhe,
  PedidoCompra,
  PedidoCompraDetalhe,
  PratoAdmin,
  PratoAdminRequest,
  SaldoEstoque,
  StatusPedido,
  UsuarioAdmin,
} from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/admin`;

  listarCategorias() {
    return this.http.get<PaginaResponse<CategoriaAdmin>>(`${this.endpoint}/categorias`, {
      params: new HttpParams().set('size', 100),
    });
  }

  salvarCategoria(id: number | null, request: CategoriaAdminRequest) {
    return id
      ? this.http.put<CategoriaAdmin>(`${this.endpoint}/categorias/${id}`, request)
      : this.http.post<CategoriaAdmin>(`${this.endpoint}/categorias`, request);
  }

  desativarCategoria(id: number) {
    return this.http.delete<void>(`${this.endpoint}/categorias/${id}`);
  }

  listarPratos() {
    return this.http.get<PaginaResponse<PratoAdmin>>(`${this.endpoint}/pratos`, {
      params: new HttpParams().set('size', 100),
    });
  }

  salvarPrato(id: number | null, request: PratoAdminRequest) {
    return id
      ? this.http.put<PratoAdmin>(`${this.endpoint}/pratos/${id}`, request)
      : this.http.post<PratoAdmin>(`${this.endpoint}/pratos`, request);
  }

  desativarPrato(id: number) {
    return this.http.delete<void>(`${this.endpoint}/pratos/${id}`);
  }

  listarIngredientes() {
    return this.http.get<PaginaResponse<IngredienteAdmin>>(`${this.endpoint}/ingredientes`, {
      params: new HttpParams().set('size', 100),
    });
  }

  salvarIngrediente(id: number | null, request: IngredienteAdminRequest) {
    return id
      ? this.http.put<IngredienteAdmin>(`${this.endpoint}/ingredientes/${id}`, request)
      : this.http.post<IngredienteAdmin>(`${this.endpoint}/ingredientes`, request);
  }

  desativarIngrediente(id: number) {
    return this.http.delete<void>(`${this.endpoint}/ingredientes/${id}`);
  }

  buscarFicha(pratoId: number) {
    return this.http.get<FichaTecnicaResponse>(`${this.endpoint}/pratos/${pratoId}/ficha`);
  }

  salvarFicha(pratoId: number, request: FichaTecnicaRequest, existente: boolean) {
    const url = `${this.endpoint}/pratos/${pratoId}/ficha`;
    return existente
      ? this.http.put<FichaTecnicaResponse>(url, request)
      : this.http.post<FichaTecnicaResponse>(url, request);
  }

  listarSaldos() {
    return this.http.get<PaginaResponse<SaldoEstoque>>(`${this.endpoint}/estoque/saldo`, {
      params: new HttpParams().set('size', 100),
    });
  }

  listarAlertas() {
    return this.http.get<PaginaResponse<SaldoEstoque>>(`${this.endpoint}/estoque/alertas`, {
      params: new HttpParams().set('size', 100),
    });
  }

  listarMovimentacoes() {
    return this.http.get<PaginaResponse<MovimentacaoEstoque>>(
      `${this.endpoint}/estoque/movimentacoes`,
      { params: new HttpParams().set('size', 50) }
    );
  }

  registrarSaida(request: {
    ingredienteId: number;
    quantidade: number;
    motivo: string;
    lote?: string;
  }) {
    return this.http.post<MovimentacaoEstoque>(
      `${this.endpoint}/estoque/movimentacao`,
      request
    );
  }

  listarFornecedores() {
    return this.http.get<PaginaResponse<Fornecedor>>(`${this.endpoint}/fornecedores`, {
      params: new HttpParams().set('size', 100),
    });
  }

  salvarFornecedor(id: number | null, request: FornecedorRequest) {
    return id
      ? this.http.put<Fornecedor>(`${this.endpoint}/fornecedores/${id}`, request)
      : this.http.post<Fornecedor>(`${this.endpoint}/fornecedores`, request);
  }

  desativarFornecedor(id: number) {
    return this.http.delete<void>(`${this.endpoint}/fornecedores/${id}`);
  }

  adicionarProdutoFornecedor(
    fornecedorId: number,
    request: { ingredienteId: number; preco: number; unidadeVenda: string }
  ) {
    return this.http.post(
      `${this.endpoint}/fornecedores/${fornecedorId}/produtos`,
      request
    );
  }

  listarCompras() {
    return this.http.get<PaginaResponse<PedidoCompra>>(`${this.endpoint}/compras`, {
      params: new HttpParams().set('size', 100),
    });
  }

  salvarCompra(id: number | null, request: {
    fornecedorId: number;
    itens: Array<{ ingredienteId: number; quantidade: number; precoUnitario: number }>;
  }) {
    return id
      ? this.http.put<PedidoCompraDetalhe>(`${this.endpoint}/compras/${id}`, request)
      : this.http.post<PedidoCompraDetalhe>(`${this.endpoint}/compras`, request);
  }

  detalharCompra(id: number) {
    return this.http.get<PedidoCompraDetalhe>(`${this.endpoint}/compras/${id}`);
  }

  enviarCompra(id: number) {
    return this.http.post(`${this.endpoint}/compras/${id}/enviar`, {});
  }

  receberCompra(id: number) {
    return this.http.post(`${this.endpoint}/compras/${id}/receber`, { itens: [] });
  }

  cancelarCompra(id: number) {
    return this.http.delete<void>(`${this.endpoint}/compras/${id}`);
  }

  listarPedidos(status?: StatusPedido | '') {
    let params = new HttpParams().set('size', 100);
    if (status) params = params.set('status', status);
    return this.http.get<PaginaResponse<PedidoAdmin>>(`${this.endpoint}/pedidos`, { params });
  }

  detalharPedido(id: number) {
    return this.http.get<PedidoAdminDetalhe>(`${this.endpoint}/pedidos/${id}`);
  }

  atualizarStatusPedido(id: number, status: StatusPedido) {
    return this.http.patch<PedidoAdminDetalhe>(`${this.endpoint}/pedidos/${id}/status`, {
      status,
      observacao: 'Atualização realizada pelo painel administrativo',
    });
  }

  cancelarPedido(id: number, motivo: string) {
    return this.http.patch<PedidoAdminDetalhe>(`${this.endpoint}/pedidos/${id}/cancelar`, {
      motivo,
    });
  }

  listarUsuarios() {
    return this.http.get<PaginaResponse<UsuarioAdmin>>(`${this.endpoint}/usuarios`, {
      params: new HttpParams().set('size', 100),
    });
  }

  criarUsuario(request: Omit<UsuarioAdmin, 'id'> & { senha: string }) {
    return this.http.post<UsuarioAdmin>(`${this.endpoint}/usuarios`, request);
  }

  atualizarUsuario(id: number, request: Omit<UsuarioAdmin, 'id'> & { senha?: string }) {
    return this.http.put<UsuarioAdmin>(`${this.endpoint}/usuarios/${id}`, request);
  }

  desativarUsuario(id: number) {
    return this.http.delete<void>(`${this.endpoint}/usuarios/${id}`);
  }
}
