import { ChangeDetectionStrategy, Component } from '@angular/core';
import { NgIcon } from '@ng-icons/core';

@Component({
  selector: 'app-trading-guide-accordion',
  imports: [NgIcon],
  templateUrl: './trading-guide-accordion.html',
  styleUrl: './trading-guide-accordion.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TradingGuideAccordionComponent {}
