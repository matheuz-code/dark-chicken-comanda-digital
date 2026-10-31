import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, Subject, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export type PerfilUsuario = 'CLIENTE' | 'ADMIN' | 'GERENTE' | 'COZINHEIRO';

export interface UsuarioAutenticado {
  id: number;
  nome: string;
  email: string;
  perfil: PerfilUsuario;
  telefone?: string;
  endereco?: string;
  status: 'ATIVO' | 'INATIVO';
}

export interface AutenticacaoResponse {
  token: string;
  tipo: 'Bearer';
  expiraEmSegundos: number;
  usuario: UsuarioAutenticado;
}

export interface CadastroClienteRequest {
  nome: string;
  email: string;
  senha: string;
  telefone: string;
  endereco: string;
}

interface SessaoPersistida extends AutenticacaoResponse {
  expiraEmEpochMs: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/auth`;
  private readonly sessionKey = 'comanda-digital.auth';
  private readonly sessionEvents = new Subject<void>();
  readonly sessaoAlterada = this.sessionEvents.asObservable();

  login(email: string, senha: string): Observable<AutenticacaoResponse> {
    return this.http
      .post<AutenticacaoResponse>(`${this.endpoint}/login`, { email, senha })
      .pipe(tap((response) => this.salvarSessao(response)));
  }

  cadastrarCliente(dados: CadastroClienteRequest): Observable<AutenticacaoResponse> {
    return this.http
      .post<AutenticacaoResponse>(`${this.endpoint}/register`, dados)
      .pipe(tap((response) => this.salvarSessao(response)));
  }

  token(): string | null {
    return this.sessaoAtual()?.token ?? null;
  }

  usuario(): UsuarioAutenticado | null {
    return this.sessaoAtual()?.usuario ?? null;
  }

  estaAutenticado(): boolean {
    return this.sessaoAtual() !== null;
  }

  temAlgumPerfil(perfis: readonly PerfilUsuario[]): boolean {
    const perfilAtual = this.usuario()?.perfil;
    return perfilAtual !== null && perfilAtual !== undefined && perfis.includes(perfilAtual);
  }

  logout(): void {
    if (typeof localStorage !== 'undefined') {
      const hadSession = localStorage.getItem(this.sessionKey) !== null;
      localStorage.removeItem(this.sessionKey);
      if (hadSession) this.sessionEvents.next();
    }
  }

  private salvarSessao(response: AutenticacaoResponse): void {
    if (typeof localStorage !== 'undefined') {
      const sessao: SessaoPersistida = {
        ...response,
        expiraEmEpochMs: Date.now() + response.expiraEmSegundos * 1000,
      };
      localStorage.setItem(this.sessionKey, JSON.stringify(sessao));
      this.sessionEvents.next();
    }
  }

  private sessaoAtual(): SessaoPersistida | null {
    if (typeof localStorage === 'undefined') return null;
    const value = localStorage.getItem(this.sessionKey);
    if (!value) return null;
    try {
      const sessao = JSON.parse(value) as Partial<SessaoPersistida>;
      if (
        !sessao.token ||
        !sessao.usuario ||
        typeof sessao.expiraEmEpochMs !== 'number' ||
        sessao.expiraEmEpochMs <= Date.now()
      ) {
        this.logout();
        return null;
      }
      return sessao as SessaoPersistida;
    } catch {
      this.logout();
      return null;
    }
  }
}
