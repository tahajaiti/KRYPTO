import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';

import { TradingApiService } from '../../api/trading.api';
import { TradingLeaderboardEntryResponse } from '../../models/trading.models';

@Component({
  selector: 'app-leaderboard-page',
  imports: [CommonModule, RouterLink, NgIcon],
  templateUrl: './leaderboard-page.html',
  styleUrl: './leaderboard-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LeaderboardPageComponent {
  private readonly tradingApi = inject(TradingApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isLoading = signal(true);
  readonly leaderboard = signal<TradingLeaderboardEntryResponse[]>([]);

  constructor() {
    this.loadLeaderboard();
  }

  private loadLeaderboard(): void {
    this.isLoading.set(true);
    this.tradingApi
      .getLeaderboard(50)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (entries) => this.leaderboard.set(entries),
        error: () => this.leaderboard.set([]),
        complete: () => this.isLoading.set(false)
      });
  }

  rankBadge(index: number): string {
    if (index === 0) return '🥇';
    if (index === 1) return '🥈';
    if (index === 2) return '🥉';
    return `#${index + 1}`;
  }
}
