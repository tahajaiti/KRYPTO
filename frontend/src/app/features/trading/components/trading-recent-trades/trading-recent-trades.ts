import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { CoinResponse } from '../../../coin/models/coin.models';
import { TradeResponse } from '../../models/trading.models';

@Component({
  selector: 'app-trading-recent-trades',
  imports: [CommonModule, NgIcon],
  templateUrl: './trading-recent-trades.html',
  styleUrl: './trading-recent-trades.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TradingRecentTradesComponent {
  trades = input.required<TradeResponse[]>();
  coins = input.required<CoinResponse[]>();

  coinSymbol(coinId: string): string {
    return this.coins().find((coin) => coin.id === coinId)?.symbol ?? coinId.slice(0, 8);
  }

  coinImage(coinId: string): string | null {
    return this.coins().find((coin) => coin.id === coinId)?.image ?? null;
  }

  coinInitial(symbol: string): string {
    return symbol.slice(0, 2).toUpperCase();
  }
}
