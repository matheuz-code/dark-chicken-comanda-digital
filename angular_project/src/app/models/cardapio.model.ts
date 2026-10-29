export interface CategoriaCardapioResponse {
  id: number;
  nome: string;
}

export interface PratoCardapioResponse {
  id: number;
  nome: string;
  descricao: string | null;
  fotoUrl: string | null;
  precoVenda: number;
  categoria: CategoriaCardapioResponse;
}

export interface PratoDetalheResponse extends PratoCardapioResponse {
  tempoPreparoMin: number;
}

export interface PaginaResponse<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
}
