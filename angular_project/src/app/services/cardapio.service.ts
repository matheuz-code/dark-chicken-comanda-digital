import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { recoverRead } from './api-recovery';
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

  listarComRecuperacao(params: ListarCardapioParams = {}, onAttempt?: (attempt: number) => void) {
    return this.listar(params).pipe(
      map((page) => {
        if (
          !page ||
          !Array.isArray(page.conteudo) ||
          page.conteudo.some(
            (dish) =>
              !dish ||
              !Number.isSafeInteger(dish.id) ||
              typeof dish.nome !== 'string' ||
              !Number.isFinite(dish.precoVenda) ||
              !dish.categoria ||
              !Number.isSafeInteger(dish.categoria.id) ||
              typeof dish.categoria.nome !== 'string',
          )
        ) {
          throw new Error('Resposta de cardápio incompatível com a API.');
        }
        return page;
      }),
      recoverRead(onAttempt),
    );
  }

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
