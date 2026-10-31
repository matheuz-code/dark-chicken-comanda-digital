import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { OrderStateService, ConfirmedOrder } from './order-state.service';
import { OrderPersistenceService } from './order-persistence.service';
import { ClientProfileStateService } from './client-profile-state.service';
import { AuthService } from './services/auth.service';

const order: ConfirmedOrder = {
  code: '42',
  backendStatus: 'RECEBIDO',
  items: [{ id: 1, name: 'Prato fictício', quantity: 1, price: 24.9, image: '' }],
  customerName: 'Cliente de teste',
  phone: '',
  address: 'Endereço fictício',
  paymentMethod: 'pix',
  orderType: 'delivery',
  sourceChannel: 'site',
  total: 24.9,
  currentStep: 0,
  kitchenStatus: 'novo',
  deliveryStatus: 'aguardando_motoboy',
  createdAtIso: '2026-10-31T10:00:00',
  estimatedPrepMinutes: 0,
};

describe('Estado de pedidos integrado', () => {
  let service: OrderStateService;
  let persistence: {
    loadConfirmedOrders: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    updateStatus: ReturnType<typeof vi.fn>;
  };
  let events: Subject<void>;
  let user: { id: number } | null;
  beforeEach(async () => {
    sessionStorage.clear();
    events = new Subject<void>();
    user = { id: 1 };
    persistence = {
      loadConfirmedOrders: vi.fn().mockResolvedValue([]),
      create: vi.fn(),
      updateStatus: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: OrderPersistenceService, useValue: persistence },
        { provide: ClientProfileStateService, useValue: { setApiOrders: vi.fn() } },
        { provide: AuthService, useValue: { sessaoAlterada: events, usuario: () => user } },
      ],
    });
    service = TestBed.inject(OrderStateService);
    await service.refresh();
    service.setItems(order.items);
  });
  afterEach(() => {
    events.complete();
    sessionStorage.clear();
  });
  it('só limpa o carrinho e confirma o pedido após a resposta da API', async () => {
    let resolve!: (value: ConfirmedOrder) => void;
    persistence.create.mockReturnValue(new Promise<ConfirmedOrder>((r) => (resolve = r)));
    const pending = service.confirmOrder({
      customerName: 'Cliente de teste',
      phone: '',
      address: order.address,
      paymentMethod: 'pix',
    });
    expect(service.confirmedOrder()).toBeNull();
    expect(service.items()).toHaveLength(1);
    resolve(order);
    await pending;
    expect(service.confirmedOrder()?.code).toBe('42');
    expect(service.items()).toEqual([]);
    expect(JSON.parse(sessionStorage.getItem('dark-chicken.cart-draft')!)).toEqual([]);
  });
  it('preserva o carrinho e não cria histórico quando o POST falha', async () => {
    persistence.create.mockRejectedValue(new Error('API indisponível'));
    await expect(
      service.confirmOrder({
        customerName: 'Cliente de teste',
        phone: '',
        address: order.address,
        paymentMethod: 'pix',
      }),
    ).rejects.toThrow();
    expect(service.items()).toHaveLength(1);
    expect(service.orders()).toEqual([]);
  });
  it('não avança o pedido local se o PATCH for recusado', async () => {
    service.orders.set([order]);
    persistence.updateStatus.mockRejectedValue(new Error('Estoque insuficiente'));
    await expect(service.confirmReceipt('42')).rejects.toThrow();
    expect(service.orders()[0].backendStatus).toBe('RECEBIDO');
  });
  it('logout limpa histórico e impede resposta pendente de restaurar dados antigos', async () => {
    let resolve!: (value: ConfirmedOrder[]) => void;
    persistence.loadConfirmedOrders.mockReturnValueOnce(
      new Promise<ConfirmedOrder[]>((r) => (resolve = r)),
    );
    const oldRead = service.refresh();
    service.orders.set([order]);
    user = null;
    events.next();
    expect(service.orders()).toEqual([]);
    expect(service.items()).toEqual([]);
    resolve([order]);
    await oldRead;
    await service.refresh();
    expect(service.orders()).toEqual([]);
    expect(service.confirmedOrder()).toBeNull();
  });
});
