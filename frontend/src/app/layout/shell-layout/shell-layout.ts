import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AppFooterComponent } from '../components/app-footer/app-footer';
import { AppHeaderComponent } from '../components/app-header/app-header';
import { TutorialOverlayComponent } from '../components/tutorial-overlay/tutorial-overlay';
import { TutorialService } from '../../core/tutorial/tutorial.service';
import { SessionStore } from '../../features/auth/state/session.store';

@Component({
  selector: 'app-shell-layout',
  imports: [RouterOutlet, AppHeaderComponent, AppFooterComponent, TutorialOverlayComponent],
  templateUrl: './shell-layout.html',
  styleUrl: './shell-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShellLayoutComponent {
  private readonly tutorial = inject(TutorialService);
  private readonly session = inject(SessionStore);

  constructor() {
    effect(() => {
      if (this.session.isAuthenticated()) {
        this.tutorial.showOnFirstLogin();
      }
    });
  }
}
