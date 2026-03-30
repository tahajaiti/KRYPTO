import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../../../core/http/models/api-response.model';
import { PageResponse } from '../../../core/http/models/page-response.model';
import {
  CoinInvestmentPreferenceResponse,
  CoinPriceHistoryPointResponse,
  CoinPriceResponse,
  CoinResponse,
  CreateCoinRequest
} from '../models/coin.models';

@Injectable({ providedIn: 'root' })
export class CoinApiService {
  private readonly http = inject(HttpClient);

  listCoins(query = '', page = 0, size = 12, sortBy = 'createdAt', direction: 'asc' | 'desc' = 'desc', activeOnly?: boolean): Observable<PageResponse<CoinResponse>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sort', `${sortBy},${direction}`);

    if (query.trim()) {
      params = params.set('query', query.trim());
    }

    if (activeOnly !== undefined) {
      params = params.set('activeOnly', String(activeOnly));
    }

    return this.http.get<PageResponse<CoinResponse>>('/api/coins', { params });
  }

  createCoin(payload: CreateCoinRequest): Observable<CoinResponse> {
    return this.http
      .post<ApiResponse<CoinResponse>>('/api/coins', payload)
      .pipe(map((response) => response.data));
  }

  getCoinById(id: string): Observable<CoinResponse> {
    return this.http
      .get<ApiResponse<CoinResponse>>(`/api/coins/${id}`)
      .pipe(map((response) => response.data));
  }

  getCoinPrice(id: string): Observable<CoinPriceResponse> {
    return this.http
      .get<ApiResponse<CoinPriceResponse>>(`/api/coins/${id}/price`)
      .pipe(map((response) => response.data));
  }

  getCoinHistory(id: string, points = 200): Observable<CoinPriceHistoryPointResponse[]> {
    return this.http
      .get<ApiResponse<CoinPriceHistoryPointResponse[]>>(`/api/coins/${id}/history?points=${points}`)
      .pipe(map((response) => response.data));
  }

  getInvestmentPreference(id: string): Observable<CoinInvestmentPreferenceResponse> {
    return this.http
      .get<ApiResponse<CoinInvestmentPreferenceResponse>>(`/api/coins/${id}/invest`)
      .pipe(map((response) => response.data));
  }

  setInvestmentPreference(id: string, investing: boolean): Observable<CoinInvestmentPreferenceResponse> {
    return this.http
      .post<ApiResponse<CoinInvestmentPreferenceResponse>>(`/api/coins/${id}/invest?investing=${investing}`, {})
      .pipe(map((response) => response.data));
  }

  getMyInvestments(): Observable<CoinInvestmentPreferenceResponse[]> {
    return this.http
      .get<ApiResponse<CoinInvestmentPreferenceResponse[]>>('/api/coins/investments/me')
      .pipe(map((response) => response.data));
  }

  getWatchedCoins(userId: string): Observable<CoinResponse[]> {
    return this.http
      .get<ApiResponse<CoinResponse[]>>(`/api/coins/investments/user/${userId}/coins`)
      .pipe(map((response) => response.data));
  }

  getCoinsByCreator(creatorId: string, page = 0, size = 20): Observable<PageResponse<CoinResponse>> {
    return this.http.get<PageResponse<CoinResponse>>(`/api/coins/creator/${creatorId}?page=${page}&size=${size}`);
  }

  updateCoinStatus(id: string, active: boolean): Observable<CoinResponse> {
    return this.http
      .put<ApiResponse<CoinResponse>>(`/api/admin/coins/${id}/status?active=${active}`, {})
      .pipe(map((response) => response.data));
  }
}
