import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ClientProfileStateService } from '../client-profile-state.service';

@Component({
  selector: 'app-cliente-perfil',
  imports: [CommonModule, RouterModule, CurrencyPipe, DatePipe],
  templateUrl: './cliente-perfil.html',
  styleUrl: './cliente-perfil.css',
})
export class ClientePerfil {
  private readonly router = inject(Router);
  protected readonly profileState = inject(ClientProfileStateService);

  protected readonly expandedOrderIds = signal<string[]>([]);
  protected readonly hasActiveOrderAlert = computed(() => this.profileState.activeOrdersCount() > 0);

  protected toggleExpand(orderId: string) {
    const expanded = this.expandedOrderIds();
    if (expanded.includes(orderId)) {
      this.expandedOrderIds.set(expanded.filter((id) => id !== orderId));
      return;
    }
    this.expandedOrderIds.set([...expanded, orderId]);
  }

  protected isExpanded(orderId: string): boolean {
    return this.expandedOrderIds().includes(orderId);
  }

  protected canTrack(orderId: string): boolean {
    const order = this.profileState.getOrderById(orderId);
    return !!order && this.profileState.isActive(order.status);
  }

  protected goToOrderTracking(orderId: string) {
    if (!this.canTrack(orderId)) return;
    this.router.navigateByUrl(`/acompanhar-pedido/${orderId}`);
  }
}
