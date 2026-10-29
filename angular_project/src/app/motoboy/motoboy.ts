import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConfirmedOrder, OrderStateService } from '../order-state.service';

@Component({
  selector: 'app-motoboy',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './motoboy.html',
  styleUrl: './motoboy.css',
})
export class Motoboy {
  private readonly router = inject(Router);
  private readonly orderState = inject(OrderStateService);

  protected titulo = 'Painel do Motoboy';
  protected busca = '';
  protected abaAtiva: 'disponiveis' | 'entregas' | 'finalizadas' = 'disponiveis';

  protected readonly pedidosDisponiveis = computed(() =>
    this.filtrarPedidos((pedido) =>
      pedido.kitchenStatus === 'finalizado' && pedido.deliveryStatus === 'aguardando_motoboy'
    )
  );
  protected readonly minhasEntregas = computed(() =>
    this.filtrarPedidos((pedido) => pedido.deliveryStatus === 'em_rota')
  );
  protected readonly finalizadas = computed(() =>
    this.filtrarPedidos((pedido) => pedido.deliveryStatus === 'entregue')
  );
  protected readonly ganhosHoje = computed(() =>
    this.finalizadas().reduce((total, pedido) => total + pedido.total, 0)
  );

  protected selecionarAba(aba: 'disponiveis' | 'entregas' | 'finalizadas'): void {
    this.abaAtiva = aba;
  }

  protected voltar(): void {
    this.router.navigateByUrl('/');
  }

  protected async aceitarPedido(code: string): Promise<void> {
    await this.orderState.updateDeliveryStatus(code, 'em_rota');
    this.abaAtiva = 'entregas';
  }

  protected async finalizarPedido(code: string): Promise<void> {
    await this.orderState.updateDeliveryStatus(code, 'entregue');
    this.abaAtiva = 'finalizadas';
  }

  protected trackByPedido(index: number, pedido: ConfirmedOrder): string {
    return pedido.code;
  }

  private filtrarPedidos(predicate: (pedido: ConfirmedOrder) => boolean): ConfirmedOrder[] {
    return this.orderState.orders().filter((pedido) => {
      if (!predicate(pedido)) return false;
      const termo = this.busca.trim().toLowerCase();
      if (!termo) return true;

      return (
        pedido.customerName.toLowerCase().includes(termo) ||
        pedido.address.toLowerCase().includes(termo) ||
        pedido.code.toLowerCase().includes(termo)
      );
    });
  }
}
