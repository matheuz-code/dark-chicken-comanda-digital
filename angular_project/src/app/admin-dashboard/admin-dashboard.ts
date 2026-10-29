import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { DashboardResponse, TopPratoResponse } from '../models/dashboard.model';
import { AuthService } from '../services/auth.service';
import { DashboardService } from '../services/dashboard.service';

const KPIS_VAZIOS: DashboardResponse = {
  data: '',
  faturamento: 0,
  totalPedidos: 0,
  ticketMedio: 0,
  custoIngredientesEstimado: 0,
  foodCostMedioPercentual: 0,
};

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboard implements OnInit {
  private readonly router = inject(Router);
  private readonly dashboardService = inject(DashboardService);
  private readonly authService = inject(AuthService);

  protected kpis: DashboardResponse = KPIS_VAZIOS;
  protected topPratos: TopPratoResponse[] = [];
  protected loading = true;
  protected errorMessage = '';

  protected chartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      {
        label: 'Unidades vendidas',
        data: [],
        backgroundColor: ['#f97316', '#fb923c', '#fdba74', '#fed7aa', '#ffedd5'],
        borderRadius: 8,
        borderSkipped: false,
      },
    ],
  };

  protected readonly chartOptions: ChartConfiguration<'bar'>['options'] = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        beginAtZero: true,
        ticks: { precision: 0, color: '#64748b' },
        grid: { color: '#f1f5f9' },
      },
      y: {
        ticks: { color: '#334155', font: { weight: 600 } },
        grid: { display: false },
      },
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        displayColors: false,
        callbacks: {
          label: (context) => `${context.parsed.x} unidade(s)`,
        },
      },
    },
  };

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    this.loading = true;
    this.errorMessage = '';
    this.dashboardService.carregar(5).subscribe({
      next: ({ kpis, topPratos }) => {
        this.kpis = kpis;
        this.topPratos = topPratos;
        this.chartData = {
          labels: topPratos.map((prato) => prato.pratoNome),
          datasets: [
            {
              ...this.chartData.datasets[0],
              data: topPratos.map((prato) => prato.quantidadeVendida),
            },
          ],
        };
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        if (error.status === 401 || error.status === 403) {
          this.authService.logout();
          this.errorMessage = 'Sua sessão não permite acessar o dashboard. Entre como administrador ou gerente.';
          return;
        }
        this.errorMessage = 'Não foi possível carregar os indicadores. Confirme se o backend está ativo.';
      },
    });
  }

  protected navigate(path: string): void {
    this.router.navigateByUrl(path);
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  protected foodCostClass(): string {
    const percentual = this.kpis.foodCostMedioPercentual;
    if (percentual <= 30) return 'healthy';
    if (percentual <= 35) return 'attention';
    return 'critical';
  }

  protected isAdmin(): boolean {
    return this.authService.usuario()?.perfil === 'ADMIN';
  }
}
