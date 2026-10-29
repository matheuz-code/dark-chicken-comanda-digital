import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../services/auth.service';
import { Login } from './login';

describe('Login registration', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Login);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture.detectChanges();
    fixture.nativeElement.querySelectorAll('.tabs__tab')[1].click();
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  function fill(id: string, value: string) {
    const input = fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function fillRegistration() {
    fill('nome', 'Cliente Teste');
    fill('email', 'teste@example.com');
    fill('telefone', '00000000000');
    fill('endereco', 'Endereco ficticio de teste');
    fill('password', 'senha-ficticia-de-teste');
    fill('confirmPassword', 'senha-ficticia-de-teste');
  }

  function submit() {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  it('creates a client through the API and saves the authenticated session before navigation', () => {
    fillRegistration();
    submit();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    const request = http.expectOne((req) => req.url.endsWith('/auth/register'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ nome: 'Cliente Teste', email: 'teste@example.com',
      telefone: '00000000000', endereco: 'Endereco ficticio de teste', senha: 'senha-ficticia-de-teste' });
    request.flush({ token: 'test-token', tipo: 'Bearer', expiraEmSegundos: 3600,
      usuario: { id: 900, nome: 'Cliente Teste', email: 'teste@example.com', perfil: 'CLIENTE', status: 'ATIVO' } });
    expect(TestBed.inject(AuthService).estaAutenticado()).toBe(true);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/cardapio');
  });

  it('shows duplicate email errors without navigating or creating a session', () => {
    fillRegistration();
    submit();
    http.expectOne((req) => req.url.endsWith('/auth/register')).flush({}, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('já está cadastrado');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(TestBed.inject(AuthService).estaAutenticado()).toBe(false);
  });

  it('blocks mismatched passwords before sending data', () => {
    fillRegistration();
    fill('confirmPassword', 'outra-senha-ficticia');
    submit();
    http.expectNone((req) => req.url.endsWith('/auth/register'));
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('não coincidem');
  });

  it('requires the customer details and does not offer staff registration', () => {
    submit();
    http.expectNone((req) => req.url.endsWith('/auth/register'));
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('campos obrigatórios');
    expect(fixture.nativeElement.querySelector('select')).toBeNull();
  });
});
