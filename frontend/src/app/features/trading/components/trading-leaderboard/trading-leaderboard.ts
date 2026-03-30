import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { TradingLeaderboardEntryResponse } from '../../models/trading.models';

@Component({
  selector: 'app-trading-leaderboard',
  imports: [CommonModule, RouterLink, NgIcon],
  templateUrl: './trading-leaderboard.html',
  styleUrl: './trading-leaderboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TradingLeaderboardComponent {
  leaderboard = input.required<TradingLeaderboardEntryResponse[]>();
}
