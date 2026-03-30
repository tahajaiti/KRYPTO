import { Injectable, signal, computed, inject } from '@angular/core';

import { AuthService } from '../../features/auth/services/auth.service';
import { SessionStore } from '../../features/auth/state/session.store';
import { SyncService } from '../services/sync.service';

export interface TutorialStep {
  title: string;
  description: string;
  icon: string;
}

const TUTORIAL_STEPS: TutorialStep[] = [
  {
    title: 'Welcome to KRYPTO!',
    description: 'KRYPTO is a simulated crypto trading platform where you can create coins, trade assets, and compete with other traders. Let\'s take a quick tour!',
    icon: '🎮'
  },
  {
    title: 'Markets',
    description: 'Browse all available coins in the Markets page. You can search, sort, and discover new tokens created by other players. Click on a coin to see its price chart and details.',
    icon: '📊'
  },
  {
    title: 'Trading',
    description: 'Place BUY and SELL orders on the Trading Desk. Choose between LIMIT orders (set your price) or MARKET orders (instant execution at current price). Watch your KRYP balance in the header!',
    icon: '⚡'
  },
  {
    title: 'Create Coins',
    description: 'Launch your own token from the Markets page! Set a name, symbol, and initial supply. Other players can then trade your coin, and its price will fluctuate based on market activity.',
    icon: '💎'
  },
  {
    title: 'Portfolio',
    description: 'Track your wallet balances, net worth, and transfer KRYP to other players from the Portfolio page. Monitor your investment performance over time.',
    icon: '💼'
  },
  {
    title: 'Profile & Watchlist',
    description: 'Keep your profile updated and build your watchlist from markets you care about most. Use the Profile page to manage settings and community discovery.',
    icon: '🎯'
  },
  {
    title: 'Leaderboards',
    description: 'Compete with other traders! Climb the trading leaderboard by executing consistent, high-quality trades.',
    icon: '🏆'
  },
  {
    title: 'You\'re Ready!',
    description: 'Start by exploring the Markets, placing your first trade, or creating your own coin. Good luck, trader! 🚀',
    icon: '🚀'
  }
];

const TUTORIAL_STORAGE_KEY = 'krypto_tutorial_completed';

@Injectable({ providedIn: 'root' })
export class TutorialService {
  private readonly authService = inject(AuthService);
  private readonly session = inject(SessionStore);
  private readonly syncService = inject(SyncService);

  readonly isOpen = signal(false);
  readonly currentStepIndex = signal(0);
  readonly steps = TUTORIAL_STEPS;

  readonly currentStep = computed(() => this.steps[this.currentStepIndex()]);
  readonly isFirstStep = computed(() => this.currentStepIndex() === 0);
  readonly isLastStep = computed(() => this.currentStepIndex() === this.steps.length - 1);
  readonly progress = computed(() => ((this.currentStepIndex() + 1) / this.steps.length) * 100);

  get isCompleted(): boolean {
    return this.session.user()?.tutorialCompleted ?? false;
  }

  startTutorial(): void {
    this.currentStepIndex.set(0);
    this.isOpen.set(true);
  }

  nextStep(): void {
    if (this.isLastStep()) {
      this.completeTutorial();
    } else {
      this.currentStepIndex.update((i) => i + 1);
    }
  }

  previousStep(): void {
    if (!this.isFirstStep()) {
      this.currentStepIndex.update((i) => i - 1);
    }
  }

  skipTutorial(): void {
    this.completeTutorial();
  }

  showOnFirstLogin(): void {
    if (!this.isCompleted) {
      this.startTutorial();
    }
  }

  private completeTutorial(): void {
    if (!this.isCompleted && this.session.isAuthenticated()) {
      this.authService.completeTutorial().subscribe({
        next: () => setTimeout(() => this.syncService.triggerGlobalRefresh(), 500)
      });
    }
    this.isOpen.set(false);
    this.currentStepIndex.set(0);
  }
}
