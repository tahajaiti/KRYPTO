import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { interval, catchError, forkJoin, of } from 'rxjs';

import { BlockchainApiService } from '../../../blockchain/api/blockchain.api';
import { BlockResponse } from '../../../blockchain/models/blockchain.models';
import { CoinApiService } from '../../../coin/api/coin.api';
import { CoinResponse } from '../../../coin/models/coin.models';
import { TradingApiService } from '../../../trading/api/trading.api';
import { TradingLeaderboardEntryResponse } from '../../../trading/models/trading.models';
import { SessionStore } from '../../../auth/state/session.store';

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, RouterLink, NgIcon],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomePageComponent {
  readonly session = inject(SessionStore);

  private readonly coinApi = inject(CoinApiService);
  private readonly tradingApi = inject(TradingApiService);
  private readonly blockchainApi = inject(BlockchainApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isLoading = signal(false);
  readonly topCoins = signal<CoinResponse[]>([]);
  readonly topTraders = signal<TradingLeaderboardEntryResponse[]>([]);
  readonly latestBlock = signal<BlockResponse | null>(null);

  constructor() {
    if (this.session.isAuthenticated()) {
      this.loadDashboard();

      interval(10000)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => {
          this.silentLoadDashboard();
        });
    }
  }

  loadDashboard(): void {
    this.isLoading.set(true);

    forkJoin({
      coinsPage: this.coinApi.listCoins('', 0, 6, 'marketCap', 'desc').pipe(catchError(() => of(null))),
      traders: this.tradingApi.getLeaderboard(5).pipe(catchError(() => of([]))),
      latestBlock: this.blockchainApi.getLatestBlock().pipe(catchError(() => of(null)))
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ coinsPage, traders, latestBlock }) => {
          this.topCoins.set(coinsPage?.content ?? []);
          this.topTraders.set(traders);
          this.latestBlock.set(latestBlock);
        },
        complete: () => {
          this.isLoading.set(false);
        }
      });
  }

  coinInitial(symbol: string): string {
    return symbol.slice(0, 2).toUpperCase();
  }

  private silentLoadDashboard(): void {
    forkJoin({
      coinsPage: this.coinApi.listCoins('', 0, 6, 'marketCap', 'desc').pipe(catchError(() => of(null))),
      traders: this.tradingApi.getLeaderboard(5).pipe(catchError(() => of([]))),
      latestBlock: this.blockchainApi.getLatestBlock().pipe(catchError(() => of(null)))
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ coinsPage, traders, latestBlock }) => {
          this.topCoins.set(coinsPage?.content ?? []);
          this.topTraders.set(traders);
          this.latestBlock.set(latestBlock);
        }
      });
  }
}
