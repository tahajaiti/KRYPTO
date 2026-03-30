import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TutorialService } from '../../../core/tutorial/tutorial.service';

@Component({
  selector: 'app-tutorial-overlay',
  imports: [CommonModule],
  template: `
    @if (tutorial.isOpen()) {
      <div class="tutorial-overlay fixed inset-0 z-50 grid place-items-center bg-slate-950/80 backdrop-blur-sm" (click)="tutorial.skipTutorial()">
        <div class="tutorial-card krypto-slide-up mx-4 w-full max-w-md rounded-2xl border border-emerald-200/20 bg-slate-900/95 p-6 shadow-2xl backdrop-blur-xl" (click)="$event.stopPropagation()">
          <!-- progress bar -->
          <div class="h-1 w-full overflow-hidden rounded-full bg-slate-800">
            <div class="h-full rounded-full bg-gradient-to-r from-emerald-400 to-cyan-400 transition-all duration-500" [style.width.%]="tutorial.progress()"></div>
          </div>

          <!-- step content -->
          <div class="mt-5 text-center">
            <p class="text-4xl">{{ tutorial.currentStep().icon }}</p>
            <h2 class="mt-3 font-['Space_Grotesk'] text-xl font-bold text-emerald-50">{{ tutorial.currentStep().title }}</h2>
            <p class="mt-2 text-sm leading-relaxed text-emerald-100/60">{{ tutorial.currentStep().description }}</p>
          </div>

          <!-- step indicator -->
          <div class="mt-5 flex items-center justify-center gap-1.5">
            @for (step of tutorial.steps; track step.title; let i = $index) {
              <span
                class="h-1.5 rounded-full transition-all duration-300"
                [ngClass]="i === tutorial.currentStepIndex() ? 'w-4 bg-emerald-400' : i < tutorial.currentStepIndex() ? 'w-1.5 bg-emerald-400/40' : 'w-1.5 bg-slate-700'"
              ></span>
            }
          </div>

          <!-- actions -->
          <div class="mt-5 flex items-center justify-between">
            <button
              class="text-xs font-medium text-emerald-100/40 transition hover:text-emerald-100/70"
              type="button"
              (click)="tutorial.skipTutorial()"
            >
              Skip tutorial
            </button>

            <div class="flex items-center gap-2">
              @if (!tutorial.isFirstStep()) {
                <button
                  class="krypto-btn-secondary px-3 py-2 text-xs"
                  type="button"
                  (click)="tutorial.previousStep()"
                >
                  ← Back
                </button>
              }
              <button
                class="krypto-btn-primary px-4 py-2 text-xs"
                type="button"
                (click)="tutorial.nextStep()"
              >
                {{ tutorial.isLastStep() ? 'Get Started! 🚀' : 'Next →' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    @reference "../../../../styles.css";

    .tutorial-overlay {
      animation: fadeIn 0.3s ease;
    }

    .tutorial-card {
      animation: slideUp 0.3s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TutorialOverlayComponent {
  readonly tutorial = inject(TutorialService);
}
