import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { WalletTransferItemResponse } from '../../models/wallet.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { 
  heroClock, heroArrowUpRight, heroArrowDownLeft, 
  heroCheckCircle, heroDocumentText, heroArrowPath 
} from '@ng-icons/heroicons/outline';

@Component({
  selector: 'app-wallet-transfer-timeline',
  standalone: true,
  imports: [CommonModule, NgIcon],
  templateUrl: './wallet-transfer-timeline.html',
  styleUrl: './wallet-transfer-timeline.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    provideIcons({ 
      heroClock, heroArrowUpRight, heroArrowDownLeft, 
      heroCheckCircle, heroDocumentText, heroArrowPath 
    })
  ]
})
export class WalletTransferTimelineComponent {
  readonly transfers = input<WalletTransferItemResponse[]>([]);
  readonly currentUserId = input<string | null>(null);
  readonly totalElements = input(0);
  readonly canLoadMore = input(false);
  readonly isLoadingMore = input(false);

  readonly loadMoreRequested = output<void>();
  readonly countLabel = computed(() => `${this.transfers().length}`);

  isOutgoingTransfer(item: WalletTransferItemResponse): boolean {
    return item.fromUserId === this.currentUserId();
  }

  requestLoadMore(): void {
    if (this.canLoadMore() && !this.isLoadingMore()) {
      this.loadMoreRequested.emit();
    }
  }
}
