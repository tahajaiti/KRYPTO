import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../../../core/http/models/api-response.model';
import { SKIP_AUTH_REFRESH } from '../../../core/http/http-context.tokens';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse>>('/api/auth/login', payload).pipe(map((response) => response.data));
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<ApiResponse<AuthResponse>>('/api/auth/register', payload)
      .pipe(map((response) => response.data));
  }

  refresh(): Observable<AuthResponse> {
    const context = new HttpContext().set(SKIP_AUTH_REFRESH, true);
    return this.http.post<ApiResponse<AuthResponse>>('/api/auth/refresh', {}, { context }).pipe(map((response) => response.data));
  }

  logout(): Observable<void> {
    const context = new HttpContext().set(SKIP_AUTH_REFRESH, true);
    return this.http
      .post<ApiResponse<null>>('/api/auth/logout', {}, { context })
      .pipe(map(() => undefined));
  }
}
