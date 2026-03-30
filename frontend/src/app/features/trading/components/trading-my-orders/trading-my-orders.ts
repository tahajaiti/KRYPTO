import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { NgIcon } from '@ng-icons/core';
import { CoinResponse } from '../../../coin/models/coin.models';
import { OrderResponse } from '../../models/trading.models';

@Component({
  selector: 'app-trading-my-orders',
  imports: [CommonModule, NgIcon],
  templateUrl: './trading-my-orders.html',
  styleUrl: './trading-my-orders.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TradingMyOrdersComponent {
  orders = input.required<OrderResponse[]>();
  coins = input.required<CoinResponse[]>();
  cancel = output<string>();

  coinSymbol(coinId: string): string {
    return this.coins().find((coin) => coin.id === coinId)?.symbol ?? coinId.slice(0, 8);
  }
}
