import {
  HttpBackend,
  HttpClient,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { extractApiErrorMessage } from '../api/api-error';
import { apiRoutes } from '../api/api-routes';
import { ToastService } from '../toast/toast.service';
import { TokenRefreshResponseDTO } from './auth.dtos';
import { SessionService } from './session.service';

let refreshRequest$: Observable<TokenRefreshResponseDTO> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const sessionService = inject(SessionService);
  const authHttp = new HttpClient(inject(HttpBackend));
  const router = inject(Router);
  const toastService = inject(ToastService);
  const isAuthRequest = req.url.includes('/auth/');
  const request = withCredentials(req);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthRequest) {
        if (!refreshRequest$) {
          refreshRequest$ = authHttp
            .post<TokenRefreshResponseDTO>(apiRoutes.auth.refresh, {}, { withCredentials: true })
            .pipe(
              tap((tokens) => sessionService.saveTokens(tokens)),
              finalize(() => {
                refreshRequest$ = null;
              }),
              shareReplay(1),
            );
        }

        return refreshRequest$.pipe(
          switchMap(() => next(withCredentials(req))),
          catchError((refreshError: unknown) => {
            redirectToLogin(sessionService, router, toastService, refreshError);
            return throwError(() => refreshError);
          }),
        );
      }

      return throwError(() => error);
    }),
  );
};

function withCredentials<T>(request: HttpRequest<T>): HttpRequest<T> {
  return request.clone({ withCredentials: true });
}

function redirectToLogin(
  sessionService: SessionService,
  router: Router,
  toastService: ToastService,
  error?: unknown,
): void {
  sessionService.clearSession();
  toastService.error(
    extractApiErrorMessage(error) ?? 'Sua sessao expirou. Faca login novamente.',
    'Sessao encerrada',
    'session-expired',
  );
  router.navigate(['/login'], {
    queryParams: { redirectTo: router.url === '/login' ? '/tasks' : router.url },
  });
}
