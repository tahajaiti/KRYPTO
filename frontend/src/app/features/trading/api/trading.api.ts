import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiResponse } from '../../../core/http/models/api-response.model';
import { PageResponse } from '../../../core/http/models/page-response.model';
import {
  OrderResponse,
  PlaceOrderRequest,
  TradeResponse,
  TradingLeaderboardEntryResponse
} from '../models/trading.models';

@Injectable({ providedIn: 'root' })
export class TradingApiService {
  private readonly http = inject(HttpClient);

  placeOrder(payload: PlaceOrderRequest): Observable<OrderResponse> {
    return this.http
      .post<ApiResponse<OrderResponse>>('/api/trades/orders', payload)
      .pipe(map((response) => response.data));
  }

  getMyOrders(page = 0, size = 20): Observable<PageResponse<OrderResponse>> {
    return this.http.get<PageResponse<OrderResponse>>(`/api/trades/orders/me?page=${page}&size=${size}`);
  }

  getMyTrades(page = 0, size = 20): Observable<PageResponse<TradeResponse>> {
    return this.http.get<PageResponse<TradeResponse>>(`/api/trades/me?page=${page}&size=${size}`);
  }

  cancelOrder(orderId: string): Observable<OrderResponse> {
    return this.http
      .post<ApiResponse<OrderResponse>>(`/api/trades/orders/${orderId}/cancel`, {})
      .pipe(map((response) => response.data));
  }

  getLeaderboard(limit = 10): Observable<TradingLeaderboardEntryResponse[]> {
    return this.http
      .get<ApiResponse<TradingLeaderboardEntryResponse[]>>(`/api/trades/leaderboard?limit=${limit}`)
      .pipe(map((response) => response.data));
  }
}
