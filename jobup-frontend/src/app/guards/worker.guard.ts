// worker.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const workerGuard: CanActivateFn = (_route, _state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.hasToken()) {
    return router.parseUrl('/sign-in');
  }
  if (auth.hasRole('ROLE_WORKER')) {
    return true;
  }
  if (auth.hasRole('ROLE_CLIENT')) {
    return router.parseUrl('/client/home');
  }
  return router.parseUrl('/forbidden');
};
