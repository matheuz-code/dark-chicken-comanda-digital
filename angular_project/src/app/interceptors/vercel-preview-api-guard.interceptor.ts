import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { throwError } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Vercel's preview URLs are generated with the `-git-<branch>` segment.
 * A preview must never use the shared production API through the rewrite in
 * vercel.json: all API calls are stopped in the browser before a request is made.
 */
export function isVercelPreviewHost(hostname: string): boolean {
  return /-git-[a-z0-9-]+\.vercel\.app$/i.test(hostname);
}

function isCurrentVercelPreview(): boolean {
  return typeof window !== 'undefined' && isVercelPreviewHost(window.location.hostname);
}

export const vercelPreviewApiGuardInterceptor: HttpInterceptorFn = (request, next) => {
  const isApiRequest = request.url === environment.apiUrl || request.url.startsWith(`${environment.apiUrl}/`);
  if (isCurrentVercelPreview() && isApiRequest) {
    return throwError(
      () =>
        new HttpErrorResponse({
          status: 403,
          statusText: 'Vercel preview',
          url: request.url,
          error: {
            mensagem:
              'A prévia não se conecta à API compartilhada. Use produção ou um ambiente de homologação isolado.',
          },
        }),
    );
  }
  return next(request);
};
