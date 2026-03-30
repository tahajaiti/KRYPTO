import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { SessionStore } from '../../features/auth/state/session.store';

export const authGuard: CanActivateFn = (_route, state) => {
  const sessionStore = inject(SessionStore);
  const router = inject(Router);

  if (sessionStore.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], { queryParams: { redirect: state.url } });
};
