import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, forkJoin, of } from 'rxjs';

import { readHttpErrorMessage } from '../../../../core/http/utils/http-error.util';
import { WalletApiService } from '../../../wallet/api/wallet.api';
import { NetWorthResponse } from '../../../wallet/models/wallet.models';
import { UserApiService } from '../../api/user.api';
import { UserLookupResponse } from '../../models/auth.models';
import { AuthService } from '../../services/auth.service';
import { SessionStore } from '../../state/session.store';
import { CoinApiService } from '../../../coin/api/coin.api';
import { CoinResponse } from '../../../coin/models/coin.models';
import { NgIcon } from '@ng-icons/core';
import { SyncService } from '../../../../core/services/sync.service';

@Component({
  selector: 'app-profile-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgIcon],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfilePageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly userApi = inject(UserApiService);
  private readonly walletApi = inject(WalletApiService);
  private readonly sessionStore = inject(SessionStore);
  private readonly coinApi = inject(CoinApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly syncService = inject(SyncService);

  readonly user = this.sessionStore.user;
  readonly isSaving = signal(false);
  readonly isSigningOut = signal(false);
  readonly saveMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly avatarLoadFailed = signal(false);
  readonly userSearchResults = signal<UserLookupResponse[]>([]);
  readonly isSearchingUsers = signal(false);
  readonly netWorth = signal<NetWorthResponse | null>(null);
  readonly watchedCoins = signal<CoinResponse[]>([]);
  readonly isLoadingWatchlist = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
    avatar: ['']
  });

  readonly profileDiscoveryForm = this.formBuilder.nonNullable.group({
    query: ['']
  });

  readonly activeTab = signal<'settings' | 'community' | 'watchlist'>('settings');

  readonly usernameDraft = toSignal(this.form.controls.username.valueChanges, { initialValue: '' });
  readonly avatarDraft = toSignal(this.form.controls.avatar.valueChanges, { initialValue: '' });

  readonly userInitials = computed(() => {
    const draft = this.usernameDraft().trim();
    const username = draft || this.user()?.username?.trim() || 'U';
    return username.slice(0, 2).toUpperCase();
  });

  readonly avatarPreview = computed(() => {
    const draft = this.avatarDraft().trim();
    if (draft) return draft;
    return this.user()?.avatar?.trim() || null;
  });

  setTab(tab: 'settings' | 'community' | 'watchlist'): void {
    this.activeTab.set(tab);
  }

  constructor() {
    effect(() => {
      const user = this.user();
      if (!user) return;

      this.form.patchValue(
        { username: user.username, avatar: user.avatar ?? '' },
        { emitEvent: false }
      );
    });

    effect(() => {
      this.avatarPreview();
      this.avatarLoadFailed.set(false);
    });

    this.profileDiscoveryForm.controls.query.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((query) => this.searchUsers(query));

    effect(() => {
      this.syncService.syncTrigger();
      const user = this.user();
      if (user) {
        this.loadProfileData(user.id);
      }
    });
  }

  onAvatarError(): void {
    this.avatarLoadFailed.set(true);
  }

  saveProfile(): void {
    if (this.form.invalid || this.isSaving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.saveMessage.set(null);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();
    const payload = {
      username: raw.username.trim(),
      avatar: raw.avatar.trim() || undefined
    };

    this.authService
      .updateProfile(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.saveMessage.set('profile updated successfully'),
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not update profile'));
          this.isSaving.set(false);
        },
        complete: () => this.isSaving.set(false)
      });
  }

  signOut(): void {
    if (this.isSigningOut()) return;

    this.isSigningOut.set(true);
    this.authService
      .logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => void this.router.navigateByUrl('/login'),
        complete: () => this.isSigningOut.set(false)
      });
  }

  private loadProfileData(userId: string): void {
    this.isLoadingWatchlist.set(true);
    forkJoin({
      netWorth: this.walletApi.getCurrentNetWorth().pipe(catchError(() => of(null as NetWorthResponse | null))),
      watchedCoins: this.coinApi.getWatchedCoins(userId).pipe(catchError(() => of([] as CoinResponse[])))
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ netWorth, watchedCoins }) => {
          this.netWorth.set(netWorth);
          this.watchedCoins.set(watchedCoins);
        },
        complete: () => this.isLoadingWatchlist.set(false)
      });
  }

  private searchUsers(rawQuery: string | null): void {
    const query = (rawQuery ?? '').trim();
    if (query.length < 2) {
      this.userSearchResults.set([]);
      this.isSearchingUsers.set(false);
      return;
    }

    this.isSearchingUsers.set(true);
    this.userApi
      .searchUsers(query, 0, 8)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => this.userSearchResults.set(page.content),
        error: () => this.userSearchResults.set([]),
        complete: () => this.isSearchingUsers.set(false)
      });
  }
}
