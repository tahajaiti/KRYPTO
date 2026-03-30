import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { BlockResponse } from '../../../blockchain/models/blockchain.models';

@Component({
  selector: 'app-trading-chain-pulse',
  imports: [],
  templateUrl: './trading-chain-pulse.html',
  styleUrl: './trading-chain-pulse.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TradingChainPulseComponent {
  block = input.required<BlockResponse>();
}
