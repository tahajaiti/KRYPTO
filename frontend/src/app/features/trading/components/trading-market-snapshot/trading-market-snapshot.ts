import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { CoinResponse } from '../../../coin/models/coin.models';

@Component({
  selector: 'app-trading-market-snapshot',
  imports: [CommonModule, RouterLink, NgIcon],
  templateUrl: './trading-market-snapshot.html',
  styleUrl: './trading-market-snapshot.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TradingMarketSnapshotComponent {
  marketCoins = input.required<CoinResponse[]>();

  coinInitial(symbol: string): string {
    return symbol.slice(0, 2).toUpperCase();
  }
}
