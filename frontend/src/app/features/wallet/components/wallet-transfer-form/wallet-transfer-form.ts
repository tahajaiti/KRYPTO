import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

import { WalletApiService } from '../../api/wallet.api';
import { RecipientLookupResponse, TransferKrypRequest, TransferResponse } from '../../models/wallet.models';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroArrowsRightLeft, heroMagnifyingGlass } from '@ng-icons/heroicons/outline';

const UUID_PATTERN = /^[0-9a-fA-F-]{36}$/;

@Component({
  selector: 'app-wallet-transfer-form',
  imports: [CommonModule, ReactiveFormsModule, NgIcon],
  templateUrl: './wallet-transfer-form.html',
  styleUrl: './wallet-transfer-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    provideIcons({ heroArrowsRightLeft, heroMagnifyingGlass })
  ]
})
export class WalletTransferFormComponent {
  private readonly walletApi = inject(WalletApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly isSubmitting = input(false);
  readonly submitMessage = input<string | null>(null);
  readonly lastTransfer = input<TransferResponse | null>(null);
  readonly resetCounter = input(0);

  readonly transferRequested = output<TransferKrypRequest>();

  readonly recipientSuggestions = signal<RecipientLookupResponse[]>([]);
  readonly selectedRecipient = signal<RecipientLookupResponse | null>(null);
  readonly isRecipientLookupLoading = signal(false);

  readonly transferForm = this.formBuilder.nonNullable.group({
    recipientQuery: ['', [Validators.required]],
    toUserId: ['', [Validators.required, Validators.pattern(UUID_PATTERN)]],
    amount: [0.01, [Validators.required, Validators.min(0.000001)]]
  });

  constructor() {
    this.transferForm.controls.recipientQuery.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap((rawQuery) => {
          const query = (rawQuery || '').trim();

          if (UUID_PATTERN.test(query)) {
            this.transferForm.controls.toUserId.setValue(query);
            this.selectedRecipient.set(null);
            this.recipientSuggestions.set([]);
          } else {
            this.transferForm.controls.toUserId.setValue('');
            this.selectedRecipient.set(null);
          }
        }),
        switchMap((rawQuery) => {
          const query = (rawQuery || '').trim();
          if (query.length < 2 || UUID_PATTERN.test(query)) {
            this.isRecipientLookupLoading.set(false);
            return of<RecipientLookupResponse[]>([]);
          }

          this.isRecipientLookupLoading.set(true);
          return this.walletApi.searchRecipients(query).pipe(
            catchError(() => of<RecipientLookupResponse[]>([]))
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((suggestions) => {
        this.recipientSuggestions.set(suggestions);
        this.isRecipientLookupLoading.set(false);
      });

    effect(() => {
      this.resetCounter();
      this.transferForm.patchValue({ recipientQuery: '', toUserId: '', amount: 0.01 }, { emitEvent: false });
      this.transferForm.markAsPristine();
      this.transferForm.markAsUntouched();
      this.recipientSuggestions.set([]);
      this.selectedRecipient.set(null);
    });
  }

  submitTransfer(): void {
    if (this.transferForm.invalid || this.isSubmitting()) {
      this.transferForm.markAllAsTouched();
      return;
    }

    const raw = this.transferForm.getRawValue();
    this.transferRequested.emit({
      toUserId: raw.toUserId.trim(),
      amount: Number(raw.amount)
    });
  }

  selectRecipient(recipient: RecipientLookupResponse): void {
    this.selectedRecipient.set(recipient);
    this.transferForm.patchValue(
      {
        recipientQuery: `${recipient.username} (${recipient.email})`,
        toUserId: recipient.id
      },
      { emitEvent: false }
    );
    this.recipientSuggestions.set([]);
  }

  clearRecipient(): void {
    this.selectedRecipient.set(null);
    this.transferForm.controls.recipientQuery.setValue('');
    this.transferForm.controls.toUserId.setValue('');
    this.recipientSuggestions.set([]);
  }
}
