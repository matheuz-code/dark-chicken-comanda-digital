import type { PerfilUsuario } from '../services/auth.service';

export type StatusAtivacao = 'ATIVO' | 'INATIVO';
export type StatusPrato = 'ATIVO' | 'INATIVO';
export type UnidadeMedida = 'UN' | 'G' | 'KG' | 'ML' | 'L';
export type StatusPedido =
  | 'RECEBIDO'
  | 'CONFIRMADO'
  | 'EM_PREPARO'
  | 'PRONTO'
  | 'SAIU_ENTREGA'
  | 'FINALIZADO'
  | 'CANCELADO';

export interface PaginaResponse<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
}

export interface CategoriaAdmin {
  id: number;
  nome: string;
  descricao?: string;
  ordem: number;
  status: StatusAtivacao;
}

export type CategoriaAdminRequest = Omit<CategoriaAdmin, 'id'>;

export interface PratoAdmin {
  id: number;
  categoria: { id: number; nome: string };
  nome: string;
  descricao?: string;
  fotoUrl?: string;
  precoVenda: number;
  tempoPreparoMin: number;
  status: StatusPrato;
}

export interface PratoAdminRequest {
  categoriaId: number;
  nome: string;
  descricao?: string;
  fotoUrl?: string;
  precoVenda: number;
  tempoPreparoMin: number;
  status: StatusPrato;
}

export interface IngredienteAdmin {
  id: number;
  nome: string;
  sku: string;
  unidadePadrao: UnidadeMedida;
  estoqueMinimo: number;
  custoUnitario: number;
  status: StatusAtivacao;
}

export type IngredienteAdminRequest = Omit<IngredienteAdmin, 'id'>;

export interface FichaTecnicaItemRequest {
  ingredienteId: number;
  quantidade: number;
  unidade: UnidadeMedida;
  fatorCorrecao: number;
}

export interface FichaTecnicaRequest {
  rendimento: number;
  modoPreparo: string;
  itens: FichaTecnicaItemRequest[];
}

export interface FichaTecnicaResponse {
  id: number;
  pratoId: number;
  nomePrato: string;
  rendimento: number;
  modoPreparo: string;
  custoUnitario: number;
  foodCostPercentual: number;
  classificacao: string;
  aviso?: string;
  itens: Array<{
    id: number;
    ingrediente: {
      id: number;
      nome: string;
      sku: string;
      unidadePadrao: UnidadeMedida;
      custoUnitario: number;
    };
    quantidade: number;
    unidade: UnidadeMedida;
    fatorCorrecao: number;
    custoComponente: number;
  }>;
  criadoEm: string;
  atualizadoEm: string;
}

export interface SaldoEstoque {
  ingredienteId: number;
  nome: string;
  sku: string;
  unidadePadrao: UnidadeMedida;
  saldoAtual: number;
  estoqueMinimo: number;
  abaixoDoMinimo: boolean;
  status: StatusAtivacao;
}

export interface MovimentacaoEstoque {
  id: number;
  ingrediente: { id: number; nome: string; sku: string; unidadePadrao: UnidadeMedida };
  tipo: 'ENTRADA' | 'SAIDA' | 'ESTORNO';
  quantidade: number;
  motivo: string;
  lote?: string;
  criadoEm: string;
  usuario?: { id: number; nome: string; perfil: PerfilUsuario };
}

export interface Fornecedor {
  id: number;
  razaoSocial: string;
  cnpj: string;
  telefone?: string;
  email?: string;
  categoriasProdutos?: string;
  status: StatusAtivacao;
}

export interface FornecedorRequest {
  razaoSocial: string;
  cnpj: string;
  telefone?: string;
  email?: string;
  categoriasProdutos?: string;
  status: StatusAtivacao;
}

export interface PedidoCompra {
  id: number;
  fornecedorId: number;
  fornecedorRazaoSocial: string;
  status: 'RASCUNHO' | 'ENVIADO' | 'RECEBIDO' | 'CANCELADO';
  valorTotal: number;
  usuarioNome: string;
  criadoEm: string;
}

export interface PedidoCompraDetalhe extends PedidoCompra {
  fornecedorCnpj: string;
  itens: Array<{
    id: number;
    ingredienteId: number;
    ingredienteNome: string;
    ingredienteSku: string;
    quantidade: number;
    unidadeCompra: UnidadeMedida;
    precoUnitario: number;
    subtotal: number;
  }>;
}

export interface PedidoAdmin {
  id: number;
  cliente: { id: number; nome: string; email: string; telefone?: string };
  canal: string;
  status: StatusPedido;
  statusPagamento: string;
  formaPagamento: string;
  valorTotal: number;
  quantidadeItens: number;
  criadoEm: string;
}

export interface PedidoAdminDetalhe {
  id: number;
  cliente: { id: number; nome: string; email: string; telefone?: string };
  canal: string;
  status: StatusPedido;
  statusPagamento: string;
  formaPagamento: string;
  valorTotal: number;
  enderecoEntrega: string;
  observacoes?: string;
  motivoCancelamento?: string;
  criadoEm: string;
  atualizadoEm: string;
  itens: Array<{
    pratoId: number;
    nomePrato: string;
    fotoUrl?: string;
    quantidade: number;
    precoUnitario: number;
    subtotal: number;
    observacoes?: string;
  }>;
  timeline: Array<{
    status: StatusPedido;
    observacao?: string;
    usuario: { id: number; nome: string; perfil: PerfilUsuario };
    registradoEm: string;
  }>;
}

export interface UsuarioAdmin {
  id: number;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  telefone?: string;
  endereco?: string;
  status: StatusAtivacao;
}
