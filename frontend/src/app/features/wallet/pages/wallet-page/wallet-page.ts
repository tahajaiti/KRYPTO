import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { 
  heroBriefcase, heroChartBar, heroWallet, heroArrowsRightLeft, 
  heroClock, heroInformationCircle, heroDocumentText, heroCurrencyDollar
} from '@ng-icons/heroicons/outline';

import { PageResponse } from '../../../../core/http/models/page-response.model';
import { readHttpErrorMessage } from '../../../../core/http/utils/http-error.util';
import { CoinApiService } from '../../../coin/api/coin.api';
import { WalletApiService } from '../../api/wallet.api';
import { WalletBalancesPanelComponent } from '../../components/wallet-balances-panel/wallet-balances-panel';
import { WalletNetWorthBreakdownPanelComponent } from '../../components/wallet-networth-breakdown-panel/wallet-networth-breakdown-panel';
import { WalletSummaryCardsComponent } from '../../components/wallet-summary-cards/wallet-summary-cards';
import { WalletTransferFormComponent } from '../../components/wallet-transfer-form/wallet-transfer-form';
import { WalletTransferTimelineComponent } from '../../components/wallet-transfer-timeline/wallet-transfer-timeline';
import {
  NetWorthResponse,
  TransferKrypRequest,
  TransferResponse,
  WalletResponse,
  WalletTransferItemResponse
} from '../../models/wallet.models';
import { SyncService } from '../../../../core/services/sync.service';

@Component({
  selector: 'app-wallet-page',
  standalone: true,
  imports: [
    CommonModule,
    NgIcon,
    WalletSummaryCardsComponent,
    WalletBalancesPanelComponent,
    WalletTransferFormComponent,
    WalletTransferTimelineComponent,
    WalletNetWorthBreakdownPanelComponent
  ],
  templateUrl: './wallet-page.html',
  styleUrl: './wallet-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    provideIcons({ 
      heroBriefcase, heroChartBar, heroWallet, heroArrowsRightLeft, 
      heroClock, heroInformationCircle, heroDocumentText, heroCurrencyDollar
    })
  ]
})
export class WalletPageComponent {
  private readonly walletApi = inject(WalletApiService);
  private readonly coinApi = inject(CoinApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly syncService = inject(SyncService);

  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly submitMessage = signal<string | null>(null);
  readonly currentTab = signal<'holdings' | 'activity' | 'insights'>('holdings');
  
  readonly wallet = signal<WalletResponse | null>(null);
  readonly netWorth = signal<NetWorthResponse | null>(null);
  readonly transferHistory = signal<WalletTransferItemResponse[]>([]);
  readonly transferPage = signal<PageResponse<WalletTransferItemResponse> | null>(null);
  readonly isLoadingMoreTransfers = signal(false);
  readonly transferFormResetCounter = signal(0);
  readonly lastTransfer = signal<TransferResponse | null>(null);
  readonly coinImages = signal<Record<string, string | null>>({});

  readonly transferTotalElements = computed(() => this.transferPage()?.totalElements ?? 0);
  readonly canLoadMoreTransfers = computed(() => {
    const page = this.transferPage();
    if (!page) return false;
    return page.hasNext && !this.isLoadingMoreTransfers();
  });

  private readonly transferPageSize = 20;

  constructor() {
    effect(() => {
      this.syncService.syncTrigger();
      this.reloadWalletData();
    });
  }

  setTab(tab: 'holdings' | 'activity' | 'insights'): void {
    this.currentTab.set(tab);
  }

  reloadWalletData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      wallet: this.walletApi.getCurrentWallet(),
      netWorth: this.walletApi.getCurrentNetWorth(),
      transferPage: this.walletApi.getCurrentTransferHistory(0, this.transferPageSize),
      coinsPage: this.coinApi.listCoins('', 0, 200, 'marketCap', 'desc')
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ wallet, netWorth, transferPage, coinsPage }) => {
          this.wallet.set(wallet);
          this.netWorth.set(netWorth);
          this.transferPage.set(transferPage);
          this.transferHistory.set(transferPage.content);

          const nextMap: Record<string, string | null> = {};
          for (const coin of coinsPage.content) {
            nextMap[coin.id] = coin.image;
          }
          this.coinImages.set(nextMap);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not load wallet data'));
        },
        complete: () => {
          this.isLoading.set(false);
        }
      });
  }

  loadMoreTransfers(): void {
    if (!this.canLoadMoreTransfers()) return;

    const currentPage = this.transferPage();
    if (!currentPage) return;

    this.isLoadingMoreTransfers.set(true);
    this.walletApi
      .getCurrentTransferHistory(currentPage.page + 1, this.transferPageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (nextPage) => {
          this.transferHistory.update((current) => [...current, ...nextPage.content]);
          this.transferPage.set(nextPage);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not load more transfers'));
        },
        complete: () => {
          this.isLoadingMoreTransfers.set(false);
        }
      });
  }

  submitTransfer(payload: TransferKrypRequest): void {
    if (this.isSubmitting()) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.submitMessage.set(null);
    this.successMessage.set(null);

    this.walletApi
      .transferKryp(payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (transfer) => {
          this.lastTransfer.set(transfer);
          this.successMessage.set('transfer completed successfully');
          this.transferFormResetCounter.update((value) => value + 1);
          this.reloadWalletData();
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'transfer failed'));
          this.isSubmitting.set(false);
        },
        complete: () => {
          this.isSubmitting.set(false);
        }
      });
  }
}
