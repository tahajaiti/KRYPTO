import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, tap } from 'rxjs';

import { AuthApiService } from '../api/auth.api';
import { UserApiService } from '../api/user.api';
import { LoginRequest, RegisterRequest, UpdateProfileRequest, UserResponse } from '../models/auth.models';
import { SessionStore } from '../state/session.store';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authApi = inject(AuthApiService);
  private readonly userApi = inject(UserApiService);
  private readonly sessionStore = inject(SessionStore);

  initializeSession(): Observable<void> {
    if (this.sessionStore.initialized()) {
      return of(undefined);
    }

    return this.userApi.getMe().pipe(
      tap((user) => this.sessionStore.setAuthenticated(user)),
      map(() => undefined),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          return this.authApi.refresh().pipe(
            tap((response) => this.sessionStore.setAuthenticated(response.user)),
            map(() => undefined),
            catchError(() => {
              this.sessionStore.setAnonymous();
              return of(undefined);
            })
          );
        }

        this.sessionStore.setAnonymous();
        return of(undefined);
      })
    );
  }

  login(payload: LoginRequest): Observable<UserResponse> {
    return this.authApi.login(payload).pipe(
      map((response) => response.user),
      tap((user) => this.sessionStore.setAuthenticated(user))
    );
  }

  register(payload: RegisterRequest): Observable<UserResponse> {
    return this.authApi.register(payload).pipe(
      map((response) => response.user),
      tap((user) => this.sessionStore.setAuthenticated(user))
    );
  }

  updateProfile(payload: UpdateProfileRequest): Observable<UserResponse> {
    return this.userApi.updateMe(payload).pipe(tap((user) => this.sessionStore.setUser(user)));
  }

  completeTutorial(): Observable<UserResponse> {
    return this.userApi.completeTutorial().pipe(tap((user) => this.sessionStore.setUser(user)));
  }

  logout(): Observable<void> {
    return this.authApi.logout().pipe(
      catchError(() => of(undefined)),
      tap(() => this.sessionStore.setAnonymous())
    );
  }
}
