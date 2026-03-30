import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { catchError, forkJoin, of } from 'rxjs';
import { TradingChainPulseComponent } from '../../components/trading-chain-pulse/trading-chain-pulse';
import { TradingGuideAccordionComponent } from '../../components/trading-guide-accordion/trading-guide-accordion';
import { TradingMarketSnapshotComponent } from '../../components/trading-market-snapshot/trading-market-snapshot';
import { TradingMyOrdersComponent } from '../../components/trading-my-orders/trading-my-orders';
import { TradingOrderFormComponent } from '../../components/trading-order-form/trading-order-form';
import { TradingRecentTradesComponent } from '../../components/trading-recent-trades/trading-recent-trades';

import { viewChild } from '@angular/core';
import { readHttpErrorMessage } from '../../../../core/http/utils/http-error.util';
import { SyncService } from '../../../../core/services/sync.service';
import { BlockchainApiService } from '../../../blockchain/api/blockchain.api';
import { BlockResponse } from '../../../blockchain/models/blockchain.models';
import { CoinApiService } from '../../../coin/api/coin.api';
import { CoinResponse } from '../../../coin/models/coin.models';
import { TradingApiService } from '../../api/trading.api';
import {
  OrderResponse,
  PlaceOrderRequest,
  TradeResponse
} from '../../models/trading.models';

@Component({
  selector: 'app-trading-page',
  imports: [
    CommonModule,
    RouterLink,
    NgIcon,
    TradingGuideAccordionComponent,
    TradingChainPulseComponent,
    TradingOrderFormComponent,
    TradingMarketSnapshotComponent,
    TradingMyOrdersComponent,
    TradingRecentTradesComponent
  ],
  templateUrl: './trading-page.html',
  styleUrl: './trading-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TradingPageComponent {
  private readonly tradingApi = inject(TradingApiService);
  private readonly coinApi = inject(CoinApiService);
  private readonly blockchainApi = inject(BlockchainApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly syncService = inject(SyncService);

  readonly orderForm = viewChild(TradingOrderFormComponent);

  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly coins = signal<CoinResponse[]>([]);
  readonly myOrders = signal<OrderResponse[]>([]);
  readonly myTrades = signal<TradeResponse[]>([]);
  readonly latestBlock = signal<BlockResponse | null>(null);

  readonly activeTab = signal<'trade' | 'orders' | 'history'>('trade');

  readonly marketCoins = computed(() => this.coins().slice(0, 12));

  constructor() {
    this.reloadTradingView();
  }

  reloadTradingView(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      coinsPage: this.coinApi.listCoins('', 0, 24, 'marketCap', 'desc'),
      ordersPage: this.tradingApi.getMyOrders(0, 20),
      tradesPage: this.tradingApi.getMyTrades(0, 20),
      latestBlock: this.blockchainApi.getLatestBlock().pipe(catchError(() => of(null)))
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ coinsPage, ordersPage, tradesPage, latestBlock }) => {
          this.coins.set(coinsPage.content);
          this.myOrders.set(ordersPage.content);
          this.myTrades.set(tradesPage.content);
          this.latestBlock.set(latestBlock);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not load trading data'));
        },
        complete: () => {
          this.isLoading.set(false);
        }
      });
  }

  dispatchPlaceOrder(payload: PlaceOrderRequest): void {
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.tradingApi
      .placeOrder(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.successMessage.set(`order ${order.id.slice(0, 8)}… placed (${order.status})`);
          this.reloadTradingView();
          setTimeout(() => this.syncService.triggerGlobalRefresh(), 500);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'failed to place order'));
          this.isSubmitting.set(false);
        },
        complete: () => {
          this.isSubmitting.set(false);
        }
      });
  }

  cancelOrder(orderId: string): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.tradingApi
      .cancelOrder(orderId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.successMessage.set('order cancelled');
          this.reloadTradingView();
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'failed to cancel order'));
        }
      });
  }
}
