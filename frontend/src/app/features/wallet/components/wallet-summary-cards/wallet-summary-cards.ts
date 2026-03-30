import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { NetWorthResponse, WalletResponse } from '../../models/wallet.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroChartBar, heroWallet, heroBriefcase, heroInformationCircle, heroClipboard } from '@ng-icons/heroicons/outline';
import { signal } from '@angular/core';

@Component({
  selector: 'app-wallet-summary-cards',
  imports: [CommonModule, NgIcon],
  templateUrl: './wallet-summary-cards.html',
  styleUrl: './wallet-summary-cards.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    provideIcons({ heroChartBar, heroWallet, heroBriefcase, heroInformationCircle, heroClipboard })
  ]
})
export class WalletSummaryCardsComponent {
  readonly wallet = input<WalletResponse | null>(null);
  readonly netWorth = input<NetWorthResponse | null>(null);

  readonly copied = signal(false);

  readonly krypBalance = computed(() => {
    return this.wallet()?.balances?.find(b => b.symbol === 'KRYP')?.balance ?? 0;
  });

  copyWalletId(): void {
    const id = this.wallet()?.id;
    if (id) {
      navigator.clipboard.writeText(id);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    }
  }
}
