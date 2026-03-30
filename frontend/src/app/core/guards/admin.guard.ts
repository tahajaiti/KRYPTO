import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { SessionStore } from '../../features/auth/state/session.store';

export const adminGuard: CanActivateFn = () => {
  const sessionStore = inject(SessionStore);
  const router = inject(Router);

  const user = sessionStore.user();
  if (user?.role === 'ADMIN') {
    return true;
  }

  return router.createUrlTree(['/']);
};
