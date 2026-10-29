export interface DashboardResponse {
  data: string;
  faturamento: number;
  totalPedidos: number;
  ticketMedio: number;
  custoIngredientesEstimado: number;
  foodCostMedioPercentual: number;
}

export interface TopPratoResponse {
  pratoId: number;
  pratoNome: string;
  quantidadeVendida: number;
  faturamento: number;
}

export interface DashboardCompleto {
  kpis: DashboardResponse;
  topPratos: TopPratoResponse[];
}
