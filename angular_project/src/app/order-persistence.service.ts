import { Injectable } from '@angular/core';
import { ConfirmedOrder } from './order-state.service';

/**
 * Persistência temporária do protótipo de pedidos.
 * Será substituída pelo endpoint REST de pedidos na fase correspondente do SRS.
 */
@Injectable({ providedIn: 'root' })
export class OrderPersistenceService {
  private readonly ordersStorageKey = 'confirmedOrders';
  private readonly lastOrderStorageKey = 'lastConfirmedOrder';

  async loadConfirmedOrders(): Promise<ConfirmedOrder[]> {
    const rawOrders = this.read(this.ordersStorageKey);
    if (!rawOrders) return [];

    try {
      const parsed = JSON.parse(rawOrders) as unknown;
      return Array.isArray(parsed) ? (parsed as ConfirmedOrder[]) : [];
    } catch {
      return [];
    }
  }

  async saveConfirmedOrders(orders: ConfirmedOrder[]): Promise<void> {
    this.write(this.ordersStorageKey, JSON.stringify(orders));

    const latestOrder = orders[0] ?? null;
    if (latestOrder) {
      this.write(this.lastOrderStorageKey, JSON.stringify(latestOrder));
    } else {
      this.remove(this.lastOrderStorageKey);
    }
  }

  async saveConfirmedOrder(order: ConfirmedOrder): Promise<void> {
    const currentOrders = await this.loadConfirmedOrders();
    const nextOrders = [
      order,
      ...currentOrders.filter((currentOrder) => currentOrder.code !== order.code),
    ];
    await this.saveConfirmedOrders(nextOrders);
  }

  async loadLastConfirmedOrder(): Promise<ConfirmedOrder | null> {
    const rawOrder = this.read(this.lastOrderStorageKey);
    if (!rawOrder) return null;

    try {
      return JSON.parse(rawOrder) as ConfirmedOrder;
    } catch {
      return null;
    }
  }

  private read(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private write(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      // O fluxo continua em memória quando o navegador bloqueia o armazenamento local.
    }
  }

  private remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      // Não há ação adicional quando o navegador bloqueia o armazenamento local.
    }
  }
}
