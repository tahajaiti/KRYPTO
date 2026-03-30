import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { interval, catchError, forkJoin, of } from 'rxjs';

import { readHttpErrorMessage } from '../../../../core/http/utils/http-error.util';
import { CoinApiService } from '../../api/coin.api';
import { CoinPriceHistoryPointResponse, CoinResponse } from '../../models/coin.models';
import { UserApiService } from '../../../auth/api/user.api';
import { UserResponse } from '../../../auth/models/auth.models';

@Component({
  selector: 'app-coin-detail-page',
  imports: [CommonModule, RouterLink, NgIcon],
  templateUrl: './coin-detail-page.html',
  styleUrl: './coin-detail-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CoinDetailPageComponent {
  private readonly coinApi = inject(CoinApiService);
  private readonly userApi = inject(UserApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly isLoading = signal(true);
  readonly isRefreshing = signal(false);
  readonly isSavingPreference = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly coin = signal<CoinResponse | null>(null);
  readonly creator = signal<UserResponse | null>(null);
  readonly livePrice = signal<number | null>(null);
  readonly history = signal<CoinPriceHistoryPointResponse[]>([]);
  readonly isInvesting = signal(false);

  readonly latestPoint = computed(() => {
    const points = this.history();
    return points.length ? points[points.length - 1] : null;
  });

  readonly highPrice = computed(() => {
    const points = this.history();
    if (!points.length) return null;
    return Math.max(...points.map((p) => p.price));
  });

  readonly lowPrice = computed(() => {
    const points = this.history();
    if (!points.length) return null;
    return Math.min(...points.map((p) => p.price));
  });

  readonly priceChangePercent = computed(() => {
    const points = this.history();
    if (points.length < 2) return 0;
    const first = points[0].price;
    const last = points[points.length - 1].price;
    if (first <= 0) return 0;
    return ((last - first) / first) * 100;
  });

  readonly totalVolume = computed(() => {
    return this.history().reduce((sum, p) => sum + p.volume, 0);
  });

  readonly chartPolyline = computed(() => {
    const points = this.history();
    if (points.length < 2) return '';
    const prices = points.map((p) => p.price);
    const min = Math.min(...prices);
    const max = Math.max(...prices);
    const width = 720;
    const height = 240;
    const xStep = width / Math.max(points.length - 1, 1);
    const range = max - min || 1;

    return points
      .map((point, index) => {
        const x = Math.round(index * xStep * 100) / 100;
        const y = Math.round((height - ((point.price - min) / range) * height) * 100) / 100;
        return `${x},${y}`;
      })
      .join(' ');
  });

  readonly chartPoints = computed(() => {
    const points = this.history();
    if (points.length < 2) return [];
    
    const prices = points.map(p => p.price);
    const min = Math.min(...prices);
    const max = Math.max(...prices);
    const range = max - min || 1;
    
    return points.map((p, i) => ({
      ...p,
      x: (i * 720) / (points.length - 1),
      y: 240 - ((p.price - min) / range) * 240
    }));
  });

  readonly hoveredPoint = signal<any>(null);

  onMouseMove(e: MouseEvent): void {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const xRatio = (e.clientX - rect.left) / rect.width;
    const pts = this.chartPoints();
    const index = Math.min(pts.length - 1, Math.max(0, Math.round(xRatio * (pts.length - 1))));
    this.hoveredPoint.set(pts[index]);
  }

  onMouseLeave(): void {
    this.hoveredPoint.set(null);
  }

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const coinId = params.get('id');
      if (!coinId) {
        this.errorMessage.set('invalid coin id');
        this.isLoading.set(false);
        return;
      }
      this.loadCoin(coinId);
    });

    interval(10000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const coinId = this.coin()?.id;
        if (coinId && !this.isRefreshing()) {
          this.silentRefresh(coinId);
        }
      });
  }

  reload(): void {
    const coinId = this.coin()?.id;
    if (!coinId || this.isRefreshing()) return;
    this.loadCoin(coinId, true);
  }

  toggleInvestment(): void {
    const coinId = this.coin()?.id;
    if (!coinId || this.isSavingPreference()) return;

    this.isSavingPreference.set(true);
    this.errorMessage.set(null);

    this.coinApi
      .setInvestmentPreference(coinId, !this.isInvesting())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.isInvesting.set(response.investing),
        error: (error: unknown) => this.errorMessage.set(readHttpErrorMessage(error, 'could not update investment preference')),
        complete: () => this.isSavingPreference.set(false)
      });
  }

  goToTrade(): void {
    const coinId = this.coin()?.id;
    if (!coinId) return;
    void this.router.navigate(['/trade'], { queryParams: { coinId, side: 'BUY', type: 'MARKET', amount: 1 } });
  }

  coinInitial(symbol: string): string {
    return symbol.slice(0, 2).toUpperCase();
  }

  private loadCoin(coinId: string, isRefresh = false): void {
    if (isRefresh) {
      this.isRefreshing.set(true);
    } else {
      this.isLoading.set(true);
    }
    this.errorMessage.set(null);

    forkJoin({
      coin: this.coinApi.getCoinById(coinId),
      price: this.coinApi.getCoinPrice(coinId),
      history: this.coinApi.getCoinHistory(coinId, 240),
      investPreference: this.coinApi.getInvestmentPreference(coinId).pipe(catchError(() => of({ coinId, investing: false })))
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ coin, price, history, investPreference }) => {
          this.coin.set(coin);
          this.livePrice.set(price.currentPrice);
          this.history.set(history);
          this.isInvesting.set(investPreference.investing);
          this.loadCreator(coin.creatorId);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not load coin details'));
        },
        complete: () => {
          this.isLoading.set(false);
          this.isRefreshing.set(false);
        }
      });
  }

  private loadCreator(creatorId: string): void {
    this.userApi
      .getUserById(creatorId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (user) => this.creator.set(user),
        error: () => this.creator.set(null)
      });
  }

  private silentRefresh(coinId: string): void {
    forkJoin({
      price: this.coinApi.getCoinPrice(coinId),
      history: this.coinApi.getCoinHistory(coinId, 240)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ price, history }) => {
          this.livePrice.set(price.currentPrice);
          this.history.set(history);
        }
      });
  }
}
