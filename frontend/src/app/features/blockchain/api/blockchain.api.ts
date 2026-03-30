import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../../../core/http/models/api-response.model';
import { PageResponse } from '../../../core/http/models/page-response.model';
import {
  AddTransactionRequest,
  BlockResponse,
  ChainValidationResponse,
  TransactionResponse
} from '../models/blockchain.models';

@Injectable({ providedIn: 'root' })
export class BlockchainApiService {
  private readonly http = inject(HttpClient);

  getLatestBlock(): Observable<BlockResponse> {
    return this.http
      .get<ApiResponse<BlockResponse>>('/api/blockchain/latest')
      .pipe(map((response) => response.data));
  }

  getBlocks(page = 0, size = 10): Observable<PageResponse<BlockResponse>> {
    return this.http.get<PageResponse<BlockResponse>>(`/api/blockchain?page=${page}&size=${size}&sort=index,desc`);
  }

  addTransaction(payload: AddTransactionRequest): Observable<TransactionResponse> {
    return this.http
      .post<ApiResponse<TransactionResponse>>('/api/blockchain/transactions', payload)
      .pipe(map((response) => response.data));
  }

  minePendingTransactions(): Observable<BlockResponse> {
    return this.http
      .post<ApiResponse<BlockResponse>>('/api/blockchain/mine', {})
      .pipe(map((response) => response.data));
  }

  verifyChain(): Observable<ChainValidationResponse> {
    return this.http
      .get<ApiResponse<ChainValidationResponse>>('/api/blockchain/verify')
      .pipe(map((response) => response.data));
  }
}
