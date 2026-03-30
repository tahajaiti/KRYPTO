import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { forkJoin, catchError, of, finalize } from 'rxjs';

import { UserApiService } from '../../../auth/api/user.api';
import { UserResponse, UserRole } from '../../../auth/models/auth.models';
import { CoinApiService } from '../../../coin/api/coin.api';
import { CoinResponse } from '../../../coin/models/coin.models';
import { TradingApiService } from '../../../trading/api/trading.api';
import { TradingLeaderboardEntryResponse } from '../../../trading/models/trading.models';
import { BlockchainApiService } from '../../../blockchain/api/blockchain.api';
import { BlockResponse, ChainValidationResponse } from '../../../blockchain/models/blockchain.models';
import { SessionStore } from '../../../auth/state/session.store';

@Component({
  selector: 'app-admin-dashboard-page',
  imports: [CommonModule, RouterLink, NgIcon],
  templateUrl: './admin-dashboard-page.html',
  styleUrl: './admin-dashboard-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminDashboardPageComponent {
  private readonly userApi = inject(UserApiService);
  private readonly coinApi = inject(CoinApiService);
  private readonly tradingApi = inject(TradingApiService);
  private readonly blockchainApi = inject(BlockchainApiService);
  private readonly sessionStore = inject(SessionStore);
  private readonly destroyRef = inject(DestroyRef);

  readonly isLoading = signal(true);
  readonly activeTab = signal<'overview' | 'users' | 'coins' | 'blockchain'>('overview');
  readonly actionLoading = signal<Record<string, boolean>>({});

  readonly users = signal<UserResponse[]>([]);
  readonly coins = signal<CoinResponse[]>([]);
  readonly topTraders = signal<TradingLeaderboardEntryResponse[]>([]);
  readonly blocks = signal<BlockResponse[]>([]);
  readonly validationResult = signal<ChainValidationResponse | null>(null);

  readonly currentUserId = computed(() => this.sessionStore.user()?.id || null);

  constructor() {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.isLoading.set(true);

    forkJoin({
      users: this.userApi.getAllUsers(0, 100).pipe(catchError(() => of({ content: [] as UserResponse[] }))),
      coins: this.coinApi.listCoins('', 0, 100, 'createdAt', 'desc', false).pipe(catchError(() => of({ content: [] as CoinResponse[] }))),
      traders: this.tradingApi.getLeaderboard(20).pipe(catchError(() => of([] as TradingLeaderboardEntryResponse[]))),
      blocks: this.blockchainApi.getBlocks(0, 10).pipe(catchError(() => of({ content: [] as BlockResponse[] })))
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: ({ users, coins, traders, blocks }) => {
          this.users.set(users.content || []);
          this.coins.set(coins.content || []);
          this.topTraders.set(traders || []);
          this.blocks.set(blocks.content || []);
        }
      });
  }

  setTab(tab: 'overview' | 'users' | 'coins' | 'blockchain'): void {
    this.activeTab.set(tab);
    if (tab === 'blockchain') {
      this.refreshBlocks();
    }
  }

  refreshBlocks(): void {
    this.blockchainApi.getBlocks(0, 10)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => this.blocks.set(res.content || []));
  }

  toggleUserStatus(user: UserResponse): void {
    if (this.actionLoading()[user.id]) return;
    this.actionLoading.update((s) => ({ ...s, [user.id]: true }));

    this.userApi.updateUserStatus(user.id, !user.enabled)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionLoading.update((s) => ({ ...s, [user.id]: false })))
      )
      .subscribe({
        next: (updated) => {
          this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        }
      });
  }

  changeUserRole(user: UserResponse, newRole: UserRole): void {
    const actionId = user.id + '-role';
    if (this.actionLoading()[actionId]) return;
    this.actionLoading.update((s) => ({ ...s, [actionId]: true }));

    this.userApi.updateUserRole(user.id, newRole)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionLoading.update((s) => ({ ...s, [actionId]: false })))
      )
      .subscribe({
        next: (updated) => {
          this.users.update((list) => list.map((u) => (u.id === updated.id ? updated : u)));
        }
      });
  }

  toggleCoinStatus(coin: CoinResponse): void {
    if (this.actionLoading()[coin.id]) return;
    this.actionLoading.update((s) => ({ ...s, [coin.id]: true }));

    this.coinApi.updateCoinStatus(coin.id, !coin.active)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionLoading.update((s) => ({ ...s, [coin.id]: false })))
      )
      .subscribe({
        next: (updated) => {
          this.coins.update((list) => list.map((c) => (c.id === updated.id ? updated : c)));
        }
      });
  }

  mineBlocks(): void {
    if (this.actionLoading()['mine']) return;
    this.actionLoading.update(s => ({ ...s, mine: true }));

    this.blockchainApi.minePendingTransactions()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionLoading.update(s => ({ ...s, mine: false })))
      )
      .subscribe({
        next: () => {
          this.refreshBlocks();
        },
        error: (err: unknown) => {
          console.error('Mining failed', err);
        }
      });
  }

  verifyChain(): void {
    if (this.actionLoading()['verify']) return;
    this.actionLoading.update(s => ({ ...s, verify: true }));

    this.blockchainApi.verifyChain()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionLoading.update(s => ({ ...s, verify: false })))
      )
      .subscribe({
        next: (result) => {
          this.validationResult.set(result);
        },
        error: (err: unknown) => {
          console.error('Verification failed', err);
        }
      });
  }
}
