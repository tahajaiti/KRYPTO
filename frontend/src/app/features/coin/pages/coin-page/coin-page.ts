import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { interval } from 'rxjs';

import { readHttpErrorMessage } from '../../../../core/http/utils/http-error.util';
import { CoinApiService } from '../../api/coin.api';
import { CoinResponse, CreateCoinRequest } from '../../models/coin.models';
import { SyncService } from '../../../../core/services/sync.service';

@Component({
  selector: 'app-coin-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgIcon],
  templateUrl: './coin-page.html',
  styleUrl: './coin-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CoinPageComponent {
  private readonly coinApi = inject(CoinApiService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly syncService = inject(SyncService);

  readonly isLoading = signal(true);
  readonly isCreating = signal(false);
  readonly isRefreshingPrice = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly showCreateModal = signal(false);

  readonly coins = signal<CoinResponse[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);

  readonly query = signal('');
  readonly sortBy = signal<'createdAt' | 'marketCap' | 'currentPrice'>('marketCap');
  readonly sortDirection = signal<'asc' | 'desc'>('desc');

  readonly selectedCoin = signal<CoinResponse | null>(null);
  readonly selectedCoinLivePrice = signal<number | null>(null);

  readonly pageLabel = computed(() => `${this.page() + 1}/${Math.max(this.totalPages(), 1)}`);

  readonly createForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    symbol: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(12)]],
    image: [''],
    initialSupply: [1000, [Validators.required, Validators.min(0.000001)]]
  });

  constructor() {
    this.loadCoins();

    interval(10000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.silentLoadCoins();
      });
  }

  loadCoins(targetPage = 0): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.coinApi
      .listCoins(this.query(), targetPage, 12, this.sortBy(), this.sortDirection(), false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.coins.set(response.content);
          this.page.set(response.page);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
          this.hasNext.set(response.hasNext);
          this.hasPrevious.set(response.hasPrevious);

          const currentSelected = this.selectedCoin();
          if (currentSelected) {
            const inPage = response.content.find((coin) => coin.id === currentSelected.id) ?? null;
            this.selectedCoin.set(inPage);
            this.selectedCoinLivePrice.set(inPage ? inPage.currentPrice : null);
          }
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not load coins'));
        },
        complete: () => {
          this.isLoading.set(false);
        }
      });
  }

  search(): void {
    this.loadCoins(0);
  }

  nextPage(): void {
    if (!this.hasNext()) return;
    this.loadCoins(this.page() + 1);
  }

  previousPage(): void {
    if (!this.hasPrevious()) return;
    this.loadCoins(this.page() - 1);
  }

  setQuery(value: string): void {
    this.query.set(value);
  }

  setSortBy(value: string): void {
    if (value === 'marketCap' || value === 'currentPrice' || value === 'createdAt') {
      this.sortBy.set(value);
    } else {
      this.sortBy.set('createdAt');
    }
    this.loadCoins(0);
  }

  toggleSortDirection(): void {
    this.sortDirection.update((current) => (current === 'asc' ? 'desc' : 'asc'));
    this.loadCoins(0);
  }

  selectCoin(coin: CoinResponse): void {
    this.selectedCoin.set(coin);
    this.selectedCoinLivePrice.set(coin.currentPrice);
  }

  openCoinDetail(coinId: string): void {
    void this.router.navigate(['/markets', coinId]);
  }

  tradeCoin(coinId: string): void {
    void this.router.navigate(['/trade'], {
      queryParams: { coinId, side: 'BUY', type: 'LIMIT', amount: 1 }
    });
  }

  refreshSelectedPrice(): void {
    const selected = this.selectedCoin();
    if (!selected || this.isRefreshingPrice()) return;

    this.isRefreshingPrice.set(true);
    this.coinApi
      .getCoinPrice(selected.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.selectedCoinLivePrice.set(response.currentPrice);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not refresh coin price'));
        },
        complete: () => {
          this.isRefreshingPrice.set(false);
        }
      });
  }

  openCreateModal(): void {
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  createCoin(): void {
    if (this.createForm.invalid || this.isCreating()) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isCreating.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const raw = this.createForm.getRawValue();
    const payload: CreateCoinRequest = {
      name: raw.name.trim(),
      symbol: raw.symbol.trim().toUpperCase(),
      initialSupply: Number(raw.initialSupply),
      image: raw.image.trim() || undefined
    };

    this.coinApi
      .createCoin(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (created) => {
          this.successMessage.set(`${created.symbol} launched successfully!`);
          this.createForm.patchValue({ name: '', symbol: '', image: '', initialSupply: 1000 });
          this.showCreateModal.set(false);
          this.loadCoins(0);
          this.selectedCoin.set(created);
          this.selectedCoinLivePrice.set(created.currentPrice);
          setTimeout(() => this.syncService.triggerGlobalRefresh(), 500);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not create coin'));
          this.isCreating.set(false);
        },
        complete: () => {
          this.isCreating.set(false);
        }
      });
  }

  coinInitial(symbol: string): string {
    return symbol.slice(0, 2).toUpperCase();
  }

  private silentLoadCoins(): void {
    this.coinApi
      .listCoins(this.query(), this.page(), 12, this.sortBy(), this.sortDirection(), false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.coins.set(response.content);
          this.totalElements.set(response.totalElements);

          const currentSelected = this.selectedCoin();
          if (currentSelected) {
            const inPage = response.content.find((c) => c.id === currentSelected.id);
            if (inPage) {
              this.selectedCoin.set(inPage);
              this.selectedCoinLivePrice.set(inPage.currentPrice);
            }
          }
        }
      });
  }
}
