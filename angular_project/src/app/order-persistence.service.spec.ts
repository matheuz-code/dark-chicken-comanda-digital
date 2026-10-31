import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OrderPersistenceService } from './order-persistence.service';
import { AuthService } from './services/auth.service';
import { environment } from '../environments/environment';

const response = {
  id: 42,
  status: 'RECEBIDO',
  valorTotal: 24.9,
  enderecoEntrega: 'Rua fictícia 100',
  formaPagamento: 'PIX',
  criadoEm: '2026-10-30T12:00:00',
  itens: [{ pratoId: 1, nomePrato: 'Burger', quantidade: 1, precoUnitario: 24.9 }],
};
const cart = [{ id: 1, name: 'Burger', quantity: 1, price: 0.01, image: '', notes: 'Sem cebola' }];

describe('Pedidos reais na API', () => {
  let service: OrderPersistenceService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { usuario: () => ({ id: 1, nome: 'Cliente fictício', perfil: 'CLIENTE' }) },
        },
      ],
    });
    service = TestBed.inject(OrderPersistenceService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('envia IDs/quantidades e utiliza total/ID do servidor, não o preço do carrinho', async () => {
    const result = service.create(cart, { address: 'Rua fictícia 100', paymentMethod: 'pix' });
    const request = http.expectOne(`${environment.apiUrl}/pedidos`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.itens).toEqual([
      { pratoId: 1, quantidade: 1, observacoes: 'Sem cebola' },
    ]);
    expect(request.request.body).not.toHaveProperty('total');
    request.flush(response);
    const order = await result;
    expect(order.code).toBe('42');
    expect(order.total).toBe(24.9);
    expect(order.backendStatus).toBe('RECEBIDO');
  });
  it('falha de POST não produz sucesso local nem repete criação', async () => {
    const result = service.create(cart, { address: 'Rua fictícia 100', paymentMethod: 'pix' });
    const expectation = expect(result).rejects.toBeTruthy();
    http
      .expectOne(`${environment.apiUrl}/pedidos`)
      .flush(
        { mensagem: 'Estoque insuficiente' },
        { status: 422, statusText: 'Unprocessable Entity' },
      );
    await expectation;
    http.expectNone(`${environment.apiUrl}/pedidos`);
  });
  it('recupera detalhes persistidos após recarga consultando a API', async () => {
    const result = service.loadConfirmedOrders();
    http
      .expectOne((req) => req.url === `${environment.apiUrl}/pedidos/meus`)
      .flush({ conteudo: [{ id: 42 }] });
    http.expectOne(`${environment.apiUrl}/pedidos/42`).flush({ ...response, status: 'PRONTO' });
    expect((await result)[0].currentStep).toBe(3);
  });
  it('muda status na API sem simular alteração local', async () => {
    const result = service.updateStatus('42', 'CONFIRMADO');
    const request = http.expectOne(`${environment.apiUrl}/admin/pedidos/42/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'CONFIRMADO' });
    request.flush({ ...response, status: 'CONFIRMADO' });
    expect((await result).currentStep).toBe(1);
  });
  it('rejeita identificador antigo local em vez de consultá-lo como pedido real', async () => {
    await expect(service.loadById('ORD-123')).rejects.toThrow('Identificador');
    http.expectNone((req) => req.url.includes('ORD-123'));
  });
  it('não apresenta resposta de uma sessão encerrada durante a consulta', async () => {
    const auth = TestBed.inject(AuthService);
    const result = service.loadById('42');
    const rejected = expect(result).rejects.toThrow('sessão mudou');
    vi.spyOn(auth, 'usuario').mockReturnValue(null);
    http.expectOne(`${environment.apiUrl}/pedidos/42`).flush(response);
    await rejected;
  });
  it('representa cancelamento sem inventar uma etapa de entrega', async () => {
    const result = service.loadById('42');
    http.expectOne(`${environment.apiUrl}/pedidos/42`).flush({ ...response, status: 'CANCELADO' });
    const order = await result;
    expect(order.backendStatus).toBe('CANCELADO');
    expect(order.currentStep).toBe(-1);
  });

  it.each([
    { criadoEm: 'data inválida' },
    { valorTotal: -1 },
    { status: '__proto__' },
    { formaPagamento: 'DESCONHECIDO' },
    { itens: [{ pratoId: 1, nomePrato: 'Prato', quantidade: 0, precoUnitario: 2 }] },
  ])('rejeita contrato JSON incompatível: %j', async (invalid) => {
    const result = service.loadById('42');
    const rejected = expect(result).rejects.toThrow('incompatível');
    http.expectOne(`${environment.apiUrl}/pedidos/42`).flush({ ...response, ...invalid });
    await rejected;
  });
  it('preserva data, itens e total do contrato de detalhe administrativo', async () => {
    const auth = TestBed.inject(AuthService);
    vi.spyOn(auth, 'usuario').mockReturnValue({
      id: 2,
      nome: 'Gestão teste',
      email: 'gestao@example.invalid',
      perfil: 'ADMIN',
      status: 'ATIVO',
    });
    const result = service.loadById('42');
    http
      .expectOne(`${environment.apiUrl}/admin/pedidos/42`)
      .flush({
        ...response,
        canal: 'WHATSAPP',
        cliente: { nome: 'Cliente fictício', telefone: '00000000000' },
      });
    const order = await result;
    expect(order.createdAtIso).toBe(response.criadoEm);
    expect(order.items[0].price).toBe(24.9);
    expect(order.sourceChannel).toBe('whatsapp');
    expect(order.customerName).toBe('Cliente fictício');
  });
});
