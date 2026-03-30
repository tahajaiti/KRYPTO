import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { readHttpErrorMessage } from '../../../../core/http/utils/http-error.util';
import { BlockchainApiService } from '../../api/blockchain.api';
import { BlockResponse, ChainValidationResponse } from '../../models/blockchain.models';

@Component({
  selector: 'app-blockchain-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './blockchain-page.html',
  styleUrl: './blockchain-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BlockchainPageComponent {
  private readonly blockchainApi = inject(BlockchainApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly isVerifying = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly latestBlock = signal<BlockResponse | null>(null);
  readonly blocks = signal<BlockResponse[]>([]);
  readonly verifyResult = signal<ChainValidationResponse | null>(null);

  readonly page = signal(0);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);
  readonly totalPages = signal(0);

  readonly pageLabel = computed(() => `${this.page() + 1}/${Math.max(this.totalPages(), 1)}`);

  readonly addTransactionForm = this.formBuilder.nonNullable.group({
    type: ['TRANSFER'],
    toUserId: [''],
    coinSymbol: ['KRYP'],
    amount: [1, [Validators.required, Validators.min(0.000001)]],
    fee: [0, [Validators.min(0)]]
  });

  constructor() {
    this.reload(0);
  }

  reload(targetPage: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      latest: this.blockchainApi.getLatestBlock(),
      page: this.blockchainApi.getBlocks(targetPage, 10)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ latest, page }) => {
          this.latestBlock.set(latest);
          this.blocks.set(page.content);
          this.page.set(page.page);
          this.hasNext.set(page.hasNext);
          this.hasPrevious.set(page.hasPrevious);
          this.totalPages.set(page.totalPages);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'could not load blockchain explorer'));
        },
        complete: () => {
          this.isLoading.set(false);
        }
      });
  }

  nextPage(): void {
    if (this.hasNext()) {
      this.reload(this.page() + 1);
    }
  }

  previousPage(): void {
    if (this.hasPrevious()) {
      this.reload(this.page() - 1);
    }
  }

  addTransaction(): void {
    if (this.addTransactionForm.invalid || this.isSubmitting()) {
      this.addTransactionForm.markAllAsTouched();
      return;
    }

    const raw = this.addTransactionForm.getRawValue();
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.blockchainApi
      .addTransaction({
        type: raw.type as 'TRANSFER' | 'COIN_CREATION' | 'TRADE' | 'REWARD' | 'MARKET_EVENT',
        toUserId: raw.toUserId.trim() || undefined,
        coinSymbol: raw.coinSymbol.trim() || undefined,
        amount: Number(raw.amount),
        fee: Number(raw.fee) > 0 ? Number(raw.fee) : undefined
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.successMessage.set('transaction queued successfully');
          this.reload(0);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'failed to add transaction'));
          this.isSubmitting.set(false);
        },
        complete: () => {
          this.isSubmitting.set(false);
        }
      });
  }

  verifyChain(): void {
    this.isVerifying.set(true);
    this.errorMessage.set(null);

    this.blockchainApi
      .verifyChain()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.verifyResult.set(result);
        },
        error: (error: unknown) => {
          this.errorMessage.set(readHttpErrorMessage(error, 'verification failed (admin-only endpoint?)'));
        },
        complete: () => {
          this.isVerifying.set(false);
        }
      });
  }
}
