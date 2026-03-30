import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { NetWorthItemResponse } from '../../models/wallet.models';

import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroChartBarSquare, heroMagnifyingGlass } from '@ng-icons/heroicons/outline';

type BreakdownSortField = 'symbol' | 'balance' | 'priceInKryp' | 'valueInKryp';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-wallet-networth-breakdown-panel',
  imports: [CommonModule, NgIcon],
  templateUrl: './wallet-networth-breakdown-panel.html',
  styleUrl: './wallet-networth-breakdown-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    provideIcons({ heroChartBarSquare, heroMagnifyingGlass })
  ]
})
export class WalletNetWorthBreakdownPanelComponent {
  readonly breakdown = input<NetWorthItemResponse[]>([]);
  readonly coinImages = input<Record<string, string | null>>({});

  readonly filter = signal('');
  readonly sortField = signal<BreakdownSortField>('valueInKryp');
  readonly sortDirection = signal<SortDirection>('desc');

  readonly sortedBreakdown = computed(() => {
    const query = this.filter().trim().toLowerCase();
    const field = this.sortField();
    const direction = this.sortDirection();

    return [...this.breakdown()]
      .filter((item) => item.symbol.toLowerCase().includes(query))
      .sort((left, right) => this.compareItems(left, right, field, direction));
  });

  setFilter(value: string): void {
    this.filter.set(value);
  }

  setSortField(value: string): void {
    this.sortField.set(this.isSortField(value) ? value : 'valueInKryp');
  }

  toggleSortDirection(): void {
    this.sortDirection.update((current) => (current === 'asc' ? 'desc' : 'asc'));
  }

  private isSortField(value: string): value is BreakdownSortField {
    return value === 'symbol' || value === 'balance' || value === 'priceInKryp' || value === 'valueInKryp';
  }

  private compareItems(
    left: NetWorthItemResponse,
    right: NetWorthItemResponse,
    field: BreakdownSortField,
    direction: SortDirection
  ): number {
    let compare = 0;

    if (field === 'symbol') {
      compare = left.symbol.localeCompare(right.symbol);
    }

    if (field === 'balance') {
      compare = left.balance - right.balance;
    }

    if (field === 'priceInKryp') {
      compare = left.priceInKryp - right.priceInKryp;
    }

    if (field === 'valueInKryp') {
      compare = left.valueInKryp - right.valueInKryp;
    }

    return direction === 'asc' ? compare : -compare;
  }

  coinImage(coinId: string): string | null {
    return this.coinImages()[coinId] ?? null;
  }

  coinInitial(symbol: string): string {
    return symbol.slice(0, 2).toUpperCase();
  }
}
