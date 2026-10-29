import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  PaginaResponse,
  PratoCardapioResponse,
  PratoDetalheResponse,
} from '../models/cardapio.model';

export interface ListarCardapioParams {
  categoriaId?: number;
  pagina?: number;
  tamanho?: number;
}

@Injectable({ providedIn: 'root' })
export class CardapioService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/cardapio`;

  listar(params: ListarCardapioParams = {}): Observable<PaginaResponse<PratoCardapioResponse>> {
    let httpParams = new HttpParams()
      .set('page', String(params.pagina ?? 0))
      .set('size', String(params.tamanho ?? 100));

    if (params.categoriaId !== undefined) {
      httpParams = httpParams.set('categoriaId', String(params.categoriaId));
    }

    return this.http.get<PaginaResponse<PratoCardapioResponse>>(this.endpoint, {
      params: httpParams,
    });
  }

  obterPorId(id: number): Observable<PratoDetalheResponse> {
    return this.http.get<PratoDetalheResponse>(`${this.endpoint}/${id}`);
  }
}
