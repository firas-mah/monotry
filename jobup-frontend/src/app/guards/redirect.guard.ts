// redirect.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NavigationDashboardService } from '../services/navigation-dashboard.service';

export const redirectGuard: CanActivateFn = (_route, _state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const nav = inject(NavigationDashboardService);

  // If not authenticated, allow access to sign-in / sign-up
  if (!auth.hasToken()) return true;

  // Authenticated → go to dashboard for current mode (if allowed)
  const mode = nav.getCurrentRoleValue();
  const hasClient = auth.hasRole('ROLE_CLIENT');
  const hasWorker = auth.hasRole('ROLE_WORKER');

  if (mode === 'worker' && hasWorker) {
    return router.parseUrl('/worker/worker-dashboard');
  }
  if (hasClient) {
    return router.parseUrl('/client/home');
  }
  return router.parseUrl('/forbidden');
};
