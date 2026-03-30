import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, of, switchMap } from 'rxjs';

import { UserApiService } from '../../api/user.api';
import { UserResponse } from '../../models/auth.models';
import { SessionStore } from '../../state/session.store';
import { TradingApiService } from '../../../trading/api/trading.api';
import { TradingLeaderboardEntryResponse } from '../../../trading/models/trading.models';
import { WalletApiService } from '../../../wallet/api/wallet.api';
import { NetWorthResponse } from '../../../wallet/models/wallet.models';
import { CoinApiService } from '../../../coin/api/coin.api';
import { CoinResponse } from '../../../coin/models/coin.models';
import { readHttpErrorMessage } from '../../../../core/http/utils/http-error.util';
import { NgIcon } from '@ng-icons/core';

@Component({
  selector: 'app-public-profile-page',
  imports: [CommonModule, RouterLink, NgIcon],
  templateUrl: './public-profile-page.html',
  styleUrl: './public-profile-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PublicProfilePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly userApi = inject(UserApiService);
  private readonly tradingApi = inject(TradingApiService);
  private readonly walletApi = inject(WalletApiService);
  private readonly coinApi = inject(CoinApiService);
  private readonly sessionStore = inject(SessionStore);
  private readonly destroyRef = inject(DestroyRef);

  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly user = signal<UserResponse | null>(null);
  readonly tradingEntry = signal<TradingLeaderboardEntryResponse | null>(null);
  readonly netWorth = signal<NetWorthResponse | null>(null);
  readonly createdCoins = signal<CoinResponse[]>([]);
  readonly watchedCoins = signal<CoinResponse[]>([]);

  readonly isSelfProfile = computed(() => {
    const currentUser = this.sessionStore.user();
    const viewed = this.user();
    return !!currentUser && !!viewed && currentUser.id === viewed.id;
  });

  constructor() {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const id = params.get('id');
        const username = params.get('username');

        if (id) {
          this.loadById(id);
          return;
        }
        if (username) {
          this.loadByUsername(username);
          return;
        }

        this.isLoading.set(false);
        this.errorMessage.set('profile identifier is missing');
      });
  }

  userInitials(username: string): string {
    return username.slice(0, 2).toUpperCase();
  }

  private loadById(id: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.userApi
      .getUserById(id)
      .pipe(
        switchMap((user) => {
          this.user.set(user);
          return this.loadExtras(user.id);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not load profile'));
          this.isLoading.set(false);
        }
      });
  }

  private loadByUsername(username: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.userApi
      .getUserByUsername(username)
      .pipe(
        switchMap((user) => {
          this.user.set(user);
          return this.loadExtras(user.id);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not load profile'));
          this.isLoading.set(false);
        }
      });
  }

  private loadExtras(userId: string) {
    return forkJoin({
      leaderboard: this.tradingApi.getLeaderboard(100).pipe(catchError(() => of([] as TradingLeaderboardEntryResponse[]))),
      netWorth: this.walletApi.getUserNetWorth(userId).pipe(catchError(() => of(null as NetWorthResponse | null))),
      createdCoins: this.coinApi.getCoinsByCreator(userId).pipe(catchError(() => of({ content: [] as CoinResponse[] }))),
      watchedCoins: this.coinApi.getWatchedCoins(userId).pipe(catchError(() => of([] as CoinResponse[])))
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap((results) => {
        this.tradingEntry.set(results.leaderboard.find((e) => e.userId === userId) ?? null);
        this.netWorth.set(results.netWorth);
        this.createdCoins.set(results.createdCoins.content);
        this.watchedCoins.set(results.watchedCoins);
        this.isLoading.set(false);
        return of(results);
      })
    );
  }
}
