import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../../../core/http/models/api-response.model';
import { PageResponse } from '../../../core/http/models/page-response.model';
import {
  NetWorthResponse,
  RecipientLookupResponse,
  TransferKrypRequest,
  TransferResponse,
  WalletTransferItemResponse,
  WalletResponse
} from '../models/wallet.models';

@Injectable({ providedIn: 'root' })
export class WalletApiService {
  private readonly http = inject(HttpClient);

  getCurrentWallet(): Observable<WalletResponse> {
    return this.http
      .get<ApiResponse<WalletResponse>>('/api/wallets/me')
      .pipe(map((response) => response.data));
  }

  getCurrentNetWorth(): Observable<NetWorthResponse> {
    return this.http
      .get<ApiResponse<NetWorthResponse>>('/api/wallets/me/net-worth')
      .pipe(map((response) => response.data));
  }

  transferKryp(payload: TransferKrypRequest): Observable<TransferResponse> {
    return this.http
      .post<ApiResponse<TransferResponse>>('/api/wallets/transfer/kryp', payload)
      .pipe(map((response) => response.data));
  }

  getCurrentTransferHistory(page = 0, size = 20): Observable<PageResponse<WalletTransferItemResponse>> {
    return this.http
      .get<PageResponse<WalletTransferItemResponse>>(`/api/wallets/me/transfers?page=${page}&size=${size}`);
  }

  searchRecipients(query: string, limit = 8): Observable<RecipientLookupResponse[]> {
    const encoded = encodeURIComponent(query);
    return this.http
      .get<PageResponse<RecipientLookupResponse>>(`/api/users/search?query=${encoded}&page=0&size=${limit}`)
      .pipe(map((response) => response.content));
  }

  getUserNetWorth(userId: string): Observable<NetWorthResponse> {
    return this.http
      .get<ApiResponse<NetWorthResponse>>(`/api/wallets/${userId}/net-worth`)
      .pipe(map((response) => response.data));
  }
}
