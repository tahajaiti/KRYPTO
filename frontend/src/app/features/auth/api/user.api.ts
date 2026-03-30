import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../../../core/http/models/api-response.model';
import { PageResponse } from '../../../core/http/models/page-response.model';
import { UpdateProfileRequest, UserLookupResponse, UserResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);

  getMe(): Observable<UserResponse> {
    return this.http.get<ApiResponse<UserResponse>>('/api/users/me').pipe(map((response) => response.data));
  }

  updateMe(payload: UpdateProfileRequest): Observable<UserResponse> {
    return this.http
      .put<ApiResponse<UserResponse>>('/api/users/me', payload)
      .pipe(map((response) => response.data));
  }

  completeTutorial(): Observable<UserResponse> {
    return this.http
      .put<ApiResponse<UserResponse>>('/api/users/me/tutorial', {})
      .pipe(map((response) => response.data));
  }

  getUserById(id: string): Observable<UserResponse> {
    return this.http
      .get<ApiResponse<UserResponse>>(`/api/users/${id}`)
      .pipe(map((response) => response.data));
  }

  getUserByUsername(username: string): Observable<UserResponse> {
    return this.http
      .get<ApiResponse<UserResponse>>(`/api/users/by-username/${encodeURIComponent(username)}`)
      .pipe(map((response) => response.data));
  }

  searchUsers(query: string, page = 0, size = 8): Observable<PageResponse<UserLookupResponse>> {
    const encoded = encodeURIComponent(query);
    return this.http.get<PageResponse<UserLookupResponse>>(`/api/users/search?query=${encoded}&page=${page}&size=${size}`);
  }

  getAllUsers(page = 0, size = 50): Observable<PageResponse<UserResponse>> {
    return this.http.get<PageResponse<UserResponse>>(`/api/users?page=${page}&size=${size}`);
  }

  updateUserStatus(id: string, enabled: boolean): Observable<UserResponse> {
    return this.http
      .put<ApiResponse<UserResponse>>(`/api/users/${id}/status?enabled=${enabled}`, {})
      .pipe(map((response) => response.data));
  }

  updateUserRole(id: string, role: 'PLAYER' | 'ADMIN'): Observable<UserResponse> {
    return this.http
      .put<ApiResponse<UserResponse>>(`/api/users/${id}/role?role=${role}`, {})
      .pipe(map((response) => response.data));
  }
}
