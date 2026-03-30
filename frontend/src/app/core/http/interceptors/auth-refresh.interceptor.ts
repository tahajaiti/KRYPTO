import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, finalize, Observable, shareReplay, switchMap, tap, throwError } from 'rxjs';

import { AuthApiService } from '../../../features/auth/api/auth.api';
import { AuthResponse } from '../../../features/auth/models/auth.models';
import { SessionStore } from '../../../features/auth/state/session.store';
import { SKIP_AUTH_REFRESH } from '../http-context.tokens';

const AUTH_PATHS_THAT_SKIP_REFRESH = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'];

let refreshInFlight$: Observable<AuthResponse> | null = null;

function shouldSkipRefresh(url: string): boolean {
  return AUTH_PATHS_THAT_SKIP_REFRESH.some((path) => url.includes(path));
}

export const authRefreshInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.context.get(SKIP_AUTH_REFRESH)) {
    return next(request);
  }

  const authApi = inject(AuthApiService);
  const sessionStore = inject(SessionStore);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      if (shouldSkipRefresh(request.url)) {
        sessionStore.setAnonymous();
        return throwError(() => error);
      }

      if (!refreshInFlight$) {
        refreshInFlight$ = authApi.refresh().pipe(
          tap((response) => sessionStore.setAuthenticated(response.user)),
          finalize(() => {
            refreshInFlight$ = null;
          }),
          shareReplay(1)
        );
      }

      return refreshInFlight$.pipe(
        switchMap(() => next(request.clone())),
        catchError((refreshError: unknown) => {
          sessionStore.setAnonymous();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
