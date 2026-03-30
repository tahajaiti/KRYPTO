import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroBriefcase, heroMagnifyingGlass } from '@ng-icons/heroicons/outline';

import { BalanceItemResponse } from '../../models/wallet.models';

type BalanceSortField = 'symbol' | 'balance';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-wallet-balances-panel',
  standalone: true,
  imports: [CommonModule, NgIcon, RouterLink],
  templateUrl: './wallet-balances-panel.html',
  styleUrl: './wallet-balances-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    provideIcons({ heroBriefcase, heroMagnifyingGlass })
  ]
})
export class WalletBalancesPanelComponent {
  readonly balances = input<BalanceItemResponse[]>([]);
  readonly coinImages = input<Record<string, string | null>>({});

  readonly filter = signal('');
  readonly sortField = signal<BalanceSortField>('symbol');
  readonly sortDirection = signal<SortDirection>('asc');

  readonly sortedBalances = computed(() => {
    const query = this.filter().trim().toLowerCase();
    const field = this.sortField();
    const direction = this.sortDirection();

    return [...this.balances()]
      .filter((item) => item.symbol.toLowerCase().includes(query))
      .sort((left, right) => {
        const valueL = field === 'symbol' ? left.symbol : Number(left.balance);
        const valueR = field === 'symbol' ? right.symbol : Number(right.balance);
        
        let compare = 0;
        if (typeof valueL === 'string' && typeof valueR === 'string') {
          compare = valueL.localeCompare(valueR);
        } else {
          compare = (valueL as number) - (valueR as number);
        }

        return direction === 'asc' ? compare : -compare;
      });
  });

  setFilter(value: string): void {
    this.filter.set(value);
  }

  setSortField(value: string): void {
    this.sortField.set(value === 'balance' ? 'balance' : 'symbol');
  }

  toggleSortDirection(): void {
    this.sortDirection.update((current) => (current === 'asc' ? 'desc' : 'asc'));
  }

  coinImage(coinId: string): string | null {
    return this.coinImages()[coinId] ?? null;
  }

  coinInitial(symbol: string): string {
    return symbol.slice(0, 2).toUpperCase();
  }
}
