import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ClientProfileStateService } from '../client-profile-state.service';
import { ConfirmedOrder, OrderStateService } from '../order-state.service';

interface Step {
  label: string;
  icon: string;
}

@Component({
  selector: 'app-acompanhar-pedido',
  imports: [CommonModule],
  templateUrl: './acompanhar-pedido.html',
  styleUrl: './acompanhar-pedido.css',
})
export class AcompanharPedido {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly orderState = inject(OrderStateService);
  private readonly profileState = inject(ClientProfileStateService);

  protected readonly order = computed<ConfirmedOrder | null>(() => {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (!orderId) {
      return this.orderState.confirmedOrder();
    }

    const stateOrder = this.orderState.getOrderByCode(orderId);
    if (stateOrder) {
      return stateOrder;
    }

    const profileOrder = this.profileState.getOrderById(orderId);
    if (!profileOrder) return null;

    // Mapeia o formato da tela de acompanhamento atual.
    const currentStep = this.mapStatusToStep(profileOrder.status);
    return {
      code: profileOrder.id,
      items: profileOrder.items,
      customerName: this.profileState.clientName(),
      address: 'Endereco cadastrado no app',
      phone: 'Telefone cadastrado no app',
      paymentMethod: 'pix',
      orderType: 'delivery',
      sourceChannel: 'site',
      total: profileOrder.total,
      currentStep,
      kitchenStatus:
        profileOrder.status === 'em_preparo'
          ? 'em_preparo'
          : profileOrder.status === 'pronto'
            ? 'finalizado'
            : 'novo',
      deliveryStatus:
        profileOrder.status === 'entregue'
          ? 'entregue'
          : profileOrder.status === 'pronto'
            ? 'aguardando_motoboy'
            : 'aguardando_motoboy',
      createdAtIso: profileOrder.createdAt.toISOString(),
      estimatedPrepMinutes: 20,
    };
  });
  protected readonly steps: Step[] = [
    { label: 'Pedido Recebido', icon: '🕒' },
    { label: 'Aceito', icon: '✅' },
    { label: 'Em Preparo', icon: '👨‍🍳' },
    { label: 'Pronto', icon: '📦' },
    { label: 'Saiu para Entrega', icon: '🛵' },
    { label: 'Entregue', icon: '🏁' },
  ];

  protected readonly progressPercent = computed(() => {
    const currentOrder = this.order();
    if (!currentOrder) return 0;
    return ((currentOrder.currentStep + 1) / this.steps.length) * 100;
  });

  protected readonly progressText = computed(() => {
    const currentOrder = this.order();
    if (!currentOrder) return '0 de 6 etapas concluidas';
    return `${currentOrder.currentStep + 1} de ${this.steps.length} etapas concluidas`;
  });

  protected readonly paymentLabel = computed(() => {
    const currentOrder = this.order();
    if (!currentOrder) return '-';
    return this.mapPaymentLabel(currentOrder.paymentMethod);
  });

  protected readonly deliveryFee = 0;
  protected readonly subtotal = computed(() => {
    const currentOrder = this.order();
    if (!currentOrder) return 0;
    return currentOrder.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  });

  protected goBack() {
    this.router.navigateByUrl('/cliente/perfil');
  }

  protected trackByStep(index: number, step: Step) {
    return step.label;
  }

  protected trackByItem(index: number, item: { id: number }) {
    return item.id;
  }

  protected isStepDone(index: number, order: ConfirmedOrder | null): boolean {
    return !!order && index <= order.currentStep;
  }

  private mapPaymentLabel(method: ConfirmedOrder['paymentMethod']) {
    if (method === 'pix') return 'Pix';
    if (method === 'cartao') return 'Cartao';
    return 'Dinheiro';
  }

  private mapStatusToStep(
    status: 'aguardando' | 'em_preparo' | 'pronto' | 'entregue' | 'cancelado'
  ): number {
    if (status === 'aguardando') return 0;
    if (status === 'em_preparo') return 2;
    if (status === 'pronto') return 3;
    if (status === 'entregue') return 5;
    return 0;
  }
}
