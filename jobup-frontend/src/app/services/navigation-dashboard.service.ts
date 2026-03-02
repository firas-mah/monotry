import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

type Mode = 'client' | 'worker';

@Injectable({
  providedIn: 'root'
})
export class NavigationDashboardService {
  private readonly STORAGE_KEY = 'app.mode';
  private currentRole$ = new BehaviorSubject<Mode>(this.restoreMode());
  
  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  private restoreMode(): Mode {
    const stored = (localStorage.getItem(this.STORAGE_KEY) as Mode | null);
    const hasClient = this.authService.hasRole('ROLE_CLIENT');
    const hasWorker = this.authService.hasRole('ROLE_WORKER');

    // Keep stored mode if it’s still allowed by current roles
    if (stored === 'client' && hasClient) return 'client';
    if (stored === 'worker' && hasWorker) return 'worker';

    // If stored isn’t valid anymore (roles changed), pick a valid one
    if (hasWorker && !hasClient) return 'worker';
    return 'client';
  }

  private persist(mode: Mode) {
    localStorage.setItem(this.STORAGE_KEY, mode);
  }
  // ---- Explicit switches (used by your toggle button) ----
  switchToClientMode(): void {
    if (!this.authService.hasRole('ROLE_CLIENT')) {
      this.router.navigateByUrl('/forbidden'); return;
    }
    this.currentRole$.next('client');
    this.persist('client');
    this.router.navigateByUrl('/client/home');
  }

  switchToWorkerMode(): void {
    if (!this.authService.hasRole('ROLE_WORKER')) {
      this.router.navigateByUrl('/forbidden'); return;
    }
    this.currentRole$.next('worker');
    this.persist('worker');
    this.router.navigateByUrl('/worker/worker-dashboard'); // adjust if your route differs
  }

  // ---- Read current mode ----
  getCurrentRole(): Observable<Mode> { return this.currentRole$.asObservable(); }
  getCurrentRoleValue(): Mode { return this.currentRole$.value; }

  // ---- Convenience navigation helpers ----
  navigateToClientPage(page: string): void {
    if (this.authService.hasRole('ROLE_CLIENT')) this.router.navigate([`/client/${page}`]);
    else this.router.navigateByUrl('/forbidden');
  }

  navigateToWorkerPage(page: string): void {
    if (this.authService.hasRole('ROLE_WORKER')) this.router.navigate([`/worker/${page}`]);
    else this.router.navigateByUrl('/forbidden');
  }

    /** Use this for unknown routes (wildcard **): redirect based on current mode + roles */
  redirectForUnknown(): void {
    const mode = this.getCurrentRoleValue();
    if (mode === 'worker' && this.authService.hasRole('ROLE_WORKER')) {
      this.router.navigateByUrl('/worker/worker-dashboard');
    } else if (this.authService.hasRole('ROLE_CLIENT')) {
      this.router.navigateByUrl('/client/home');
    } else {
      this.router.navigateByUrl('/forbidden');
    }
  }
}