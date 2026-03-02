// client.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const clientGuard: CanActivateFn = (_route, _state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.hasToken()) {
    return router.parseUrl('/sign-in');
  }
  if (auth.hasRole('ROLE_CLIENT')) {
    return true;
  }
  // If user is worker-only, send to worker dashboard (correct path)
  if (auth.hasRole('ROLE_WORKER')) {
    return router.parseUrl('/worker/worker-dashboard');
  }
  return router.parseUrl('/forbidden');
};
