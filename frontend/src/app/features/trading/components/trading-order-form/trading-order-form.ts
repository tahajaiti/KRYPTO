import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NgIcon } from '@ng-icons/core';
import { startWith } from 'rxjs';
import { CoinResponse } from '../../../coin/models/coin.models';
import { PlaceOrderRequest } from '../../models/trading.models';

@Component({
  selector: 'app-trading-order-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgIcon],
  templateUrl: './trading-order-form.html',
  styleUrl: './trading-order-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TradingOrderFormComponent {
  coins = input.required<CoinResponse[]>();
  isSubmitting = input.required<boolean>();
  preSelectedCoinId = input<string | null>(null);
  placeOrder = output<PlaceOrderRequest>();

  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  readonly orderForm = this.formBuilder.nonNullable.group({
    coinId: ['', [Validators.required]],
    type: ['LIMIT', [Validators.required]],
    side: ['BUY', [Validators.required]],
    price: [0.01, [Validators.min(0.000001)]],
    amount: [1, [Validators.required, Validators.min(0.000001)]]
  });

  readonly coinIdSignal = toSignal(
    this.orderForm.controls.coinId.valueChanges.pipe(startWith('')),
    { initialValue: '' }
  );
  readonly typeSignal = toSignal(
    this.orderForm.controls.type.valueChanges.pipe(startWith('LIMIT')),
    { initialValue: 'LIMIT' }
  );
  readonly sideSignal = toSignal(
    this.orderForm.controls.side.valueChanges.pipe(startWith('BUY')),
    { initialValue: 'BUY' }
  );

  readonly coinSearchQuery = signal('');
  readonly coinDropdownOpen = signal(false);

  readonly searchInput = viewChild<ElementRef<HTMLInputElement>>('searchInput');
  readonly amountInput = viewChild<ElementRef<HTMLInputElement>>('amountInput');

  readonly selectedCoin = computed(() => {
    const id = this.coinIdSignal();
    return this.coins().find((c) => c.id === id) ?? null;
  });

  readonly filteredCoins = computed(() => {
    const query = this.coinSearchQuery().toLowerCase().trim();
    const allCoins = this.coins();
    if (!query) return allCoins;
    return allCoins.filter(
      (c) => c.symbol.toLowerCase().includes(query) || c.name.toLowerCase().includes(query)
    );
  });

  private prefillApplied = false;

  constructor() {
    effect(() => {
      const allCoins = this.coins();
      const externalCoinId = this.preSelectedCoinId();
      
      if (allCoins.length === 0) return;

      if (externalCoinId && allCoins.some(c => c.id === externalCoinId)) {
        this.orderForm.controls.coinId.setValue(externalCoinId);
      }

      if (!this.prefillApplied) {
        this.applyTradeIntentFromQuery(allCoins);
        this.prefillApplied = true;
      }
      if (!this.orderForm.controls.coinId.value && allCoins.length > 0) {
        this.orderForm.controls.coinId.setValue(allCoins[0].id);
      }
    }, { allowSignalWrites: true });
  }

  focusAmountInput(): void {
    setTimeout(() => this.amountInput()?.nativeElement.focus(), 100);
  }

  openCoinDropdown(): void {
    this.coinDropdownOpen.set(true);
    this.coinSearchQuery.set('');
    setTimeout(() => this.searchInput()?.nativeElement.focus(), 0);
  }

  closeCoinDropdown(): void {
    setTimeout(() => this.coinDropdownOpen.set(false), 150);
  }

  selectCoin(coin: CoinResponse): void {
    this.orderForm.controls.coinId.setValue(coin.id);
    this.coinDropdownOpen.set(false);
    this.coinSearchQuery.set('');
  }

  coinInitial(symbol: string): string {
    return symbol.slice(0, 2).toUpperCase();
  }

  setOrderType(value: string): void {
    const isMarket = value === 'MARKET';
    this.orderForm.controls.type.setValue(isMarket ? 'MARKET' : 'LIMIT');
    if (isMarket) {
      this.orderForm.controls.price.setValue(0.01);
      this.orderForm.controls.price.disable({ emitEvent: false });
    } else {
      this.orderForm.controls.price.enable({ emitEvent: false });
    }
  }

  submitOrder(): void {
    if (this.orderForm.invalid || this.isSubmitting()) {
      this.orderForm.markAllAsTouched();
      return;
    }

    const raw = this.orderForm.getRawValue();
    const payload: PlaceOrderRequest = {
      coinId: raw.coinId,
      type: raw.type as any,
      side: raw.side as any,
      amount: Number(raw.amount),
      price: raw.type === 'LIMIT' ? Number(raw.price) : undefined
    };

    this.placeOrder.emit(payload);
  }

  private applyTradeIntentFromQuery(allCoins: CoinResponse[]): void {
    const queryMap = this.route.snapshot.queryParamMap;
    const coinId = queryMap.get('coinId');
    const side = queryMap.get('side');
    const type = queryMap.get('type');
    const amount = Number(queryMap.get('amount'));

    if (coinId && allCoins.some((coin) => coin.id === coinId)) {
      this.orderForm.controls.coinId.setValue(coinId);
    }

    if (side === 'BUY' || side === 'SELL') {
      this.orderForm.controls.side.setValue(side);
    }

    if (type === 'MARKET' || type === 'LIMIT') {
      this.setOrderType(type);
    }

    if (!Number.isNaN(amount) && amount > 0) {
      this.orderForm.controls.amount.setValue(amount);
    }
  }
}
