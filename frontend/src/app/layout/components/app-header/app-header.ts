import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgIcon } from '@ng-icons/core';

import { AuthService } from '../../../features/auth/services/auth.service';
import { SessionStore } from '../../../features/auth/state/session.store';
import { WalletApiService } from '../../../features/wallet/api/wallet.api';
import { SyncService } from '../../../core/services/sync.service';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterLink, RouterLinkActive, NgIcon],
  templateUrl: './app-header.html',
  styleUrl: './app-header.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppHeaderComponent {
  private readonly authService = inject(AuthService);
  private readonly walletApi = inject(WalletApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly syncService = inject(SyncService);

  readonly session = inject(SessionStore);
  readonly user = this.session.user;
  readonly isMobileMenuOpen = signal(false);
  readonly krypBalance = signal<number | null>(null);

  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly userInitials = computed(() => {
    const username = this.user()?.username?.trim();
    if (!username) {
      return 'U';
    }

    return username.slice(0, 2).toUpperCase();
  });

  readonly formattedBalance = computed(() => {
    const balance = this.krypBalance();
    if (balance === null) return '...';
    if (balance >= 1000000) return (balance / 1000000).toFixed(1) + 'M';
    if (balance >= 1000) return (balance / 1000).toFixed(1) + 'K';
    return balance.toFixed(2);
  });

  constructor() {
    effect(() => {
      this.syncService.syncTrigger(); // observe global refreshes

      if (!this.session.isAuthenticated()) {
        this.krypBalance.set(null);
        return;
      }

      this.loadKrypBalance();
    });
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update((open) => !open);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  signOut(): void {
    this.authService
      .logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.closeMobileMenu();
        void this.router.navigateByUrl('/login');
      });
  }

  private loadKrypBalance(): void {
    this.walletApi
      .getCurrentWallet()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (wallet) => {
          const krypEntry = wallet.balances.find((b) => b.symbol === 'KRYP');
          this.krypBalance.set(krypEntry?.balance ?? 0);
        },
        error: () => {
          this.krypBalance.set(null);
        }
      });
  }
}
