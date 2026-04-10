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
import { apiRoutes } from '../api/api-routes';
import { SessionService } from './session.service';
import { TokenRefreshResponseDTO } from './auth.dtos';

let refreshRequest$: Observable<TokenRefreshResponseDTO> | null = null;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const sessionService = inject(SessionService);
  const authHttp = new HttpClient(inject(HttpBackend));
  const router = inject(Router);
  const token = sessionService.getToken();
  const isAuthRequest = req.url.includes('/auth/');

  const request = token ? withBearerToken(req, token) : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthRequest) {
        const refreshToken = sessionService.getRefreshToken();

        if (!refreshToken) {
          redirectToLogin(sessionService, router);
          return throwError(() => error);
        }

        if (!refreshRequest$) {
          refreshRequest$ = authHttp
            .post<TokenRefreshResponseDTO>(apiRoutes.auth.refresh, { refreshToken })
            .pipe(
              tap((tokens) => sessionService.saveTokens(tokens)),
              finalize(() => {
                refreshRequest$ = null;
              }),
              shareReplay(1),
            );
        }

        return refreshRequest$.pipe(
          switchMap((tokens) => next(withBearerToken(req, tokens.token))),
          catchError((refreshError: unknown) => {
            redirectToLogin(sessionService, router);
            return throwError(() => refreshError);
          }),
        );
      }

      return throwError(() => error);
    }),
  );
};

function withBearerToken<T>(request: HttpRequest<T>, token: string): HttpRequest<T> {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}

function redirectToLogin(sessionService: SessionService, router: Router): void {
  sessionService.clearSession();
  router.navigate(['/login'], {
    queryParams: { redirectTo: router.url === '/login' ? '/tasks' : router.url },
  });
}
