import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  DashboardCompleto,
  DashboardResponse,
  TopPratoResponse,
} from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/admin/dashboard`;

  carregar(limit = 5): Observable<DashboardCompleto> {
    const params = new HttpParams().set('limit', String(limit));
    return forkJoin({
      kpis: this.http.get<DashboardResponse>(this.endpoint),
      topPratos: this.http.get<TopPratoResponse[]>(`${this.endpoint}/top-pratos`, {
        params,
      }),
    });
  }
}
