import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap, catchError, throwError } from 'rxjs';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import {
  AuthenticationService,
  AuthResponseDto,
  LoginRequestDto,
  RegisterRequestDto
} from '../generated-sources/openapi';

// Replace your existing ContextRole type with these:
type BusinessRole = 'CLIENT' | 'WORKER';
type ContextRole  = BusinessRole | 'UNKNOWN';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'jobup_token';
  private readonly USER_KEY  = 'jobup_user';
  private readonly MODE_KEY  = 'app.mode'; // 'client' | 'worker'

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  private currentUserSubject     = new BehaviorSubject<any>(this.getCurrentUser());

  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public currentUser$     = this.currentUserSubject.asObservable();

  // ✨ Context role derived from URL (client/worker)
  private contextRoleSubject = new BehaviorSubject<ContextRole>('UNKNOWN');
  public  contextRole$       = this.contextRoleSubject.asObservable();

  constructor(
    private authController: AuthenticationService,
    private router: Router
  ) {
    // initialize from current url
    this.contextRoleSubject.next(this.inferRoleFromUrl(this.router.url));
    // keep it updated on navigation
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        this.contextRoleSubject.next(this.inferRoleFromUrl(this.router.url));
      });
  }

  // ---------- Auth API ----------

  register(registerData: RegisterRequestDto): Observable<AuthResponseDto> {
    return this.authController.register(registerData).pipe(
      tap(async response => await this.handleAuthSuccess(response))
    );
  }

  login(loginData: LoginRequestDto): Observable<AuthResponseDto> {
    return this.authController.login(loginData).pipe(
      tap(async response => { await this.handleAuthSuccess(response); }),
      catchError((error: any) => {
        console.error('Login failed:', error);
        return throwError(() => error);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.isAuthenticatedSubject.next(false);
    this.currentUserSubject.next(null);
    this.router.navigate(['/sign-in']);
  }

  // ---------- Storage helpers ----------

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  hasToken(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): any {
    const userStr = localStorage.getItem(this.USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
  }

  getCurrentUserId(): string | undefined {
    const user = this.getCurrentUser();
    return user?.userId;
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user?.roles?.includes(role) || false;
  }

  /** Permission check based on granted authorities (keep existing behavior) */
  isWorker(): boolean {
    return this.hasRole('ROLE_WORKER');
  }

  updateCurrentUserData(updatedUserData: any): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(updatedUserData));
    this.currentUserSubject.next(updatedUserData);
    console.log('Current user data updated:', updatedUserData);
  }

  updateToken(newToken: string): void {
    localStorage.setItem(this.TOKEN_KEY, newToken);
    this.isAuthenticatedSubject.next(true);
    console.log('Token updated successfully');
  }

  refreshUserData(): Observable<any> {
    return this.authController.getCurrentUser().pipe(
      tap(response => {
        const currentUser = this.getCurrentUser();
        const userData = {
          roles: response.roles || [],
          userId: currentUser?.userId || response.userId,
          username: currentUser?.username || response.username,
          email: currentUser?.email || response.email
        };
        localStorage.setItem(this.USER_KEY, JSON.stringify(userData));
        this.currentUserSubject.next(userData);
        console.log('User data refreshed:', userData);
      }),
      catchError((error: any) => {
        console.error('Error refreshing user data:', error);
        return throwError(() => error);
      })
    );
  }

  // ---------- Context role logic ----------

  /** What the user is acting as on this page (from URL) */
  getContextRole(): ContextRole {
    return this.contextRoleSubject.value;
  }

  /** Preferred mode persisted locally: 'client' | 'worker' */
  setPreferredMode(mode: 'client' | 'worker') {
    localStorage.setItem(this.MODE_KEY, mode);
  }
  private getPreferredMode(): 'client' | 'worker' | null {
    const m = localStorage.getItem(this.MODE_KEY);
    return m === 'client' || m === 'worker' ? m : null;
  }

  /**
   * Acting role to use in components/services:
   * - Prefer context from URL (/client, /worker)
   * - Else prefer stored mode if compatible with granted roles
   * - Else derive from roles
   */
/** Acting role to use in components */
  getBusinessRole(): BusinessRole {
    const ctx = this.getContextRole();
    if (ctx !== 'UNKNOWN') return ctx;

    const hasClient = this.hasRole('ROLE_CLIENT');
    const hasWorker = this.hasRole('ROLE_WORKER');

    const pref = this.getPreferredMode(); // 'client' | 'worker' | null
    if (pref === 'worker' && hasWorker) return 'WORKER';
    if (pref === 'client' && hasClient) return 'CLIENT';

    // fallback by roles
    if (hasWorker && !hasClient) return 'WORKER';
    return 'CLIENT';
  }

  /** Convenience: acting as worker right now? */
  isActingAsWorker(): boolean {
    return this.getBusinessRole() === 'WORKER';
  }

  private inferRoleFromUrl(url: string): ContextRole {
    const first = url.split('?')[0].split('#')[0]
      .split('/')
      .filter(Boolean)[0]?.toLowerCase();

    if (first === 'client') return 'CLIENT';
    if (first === 'worker') return 'WORKER';
    return 'UNKNOWN';
  }

  // ---------- Post-auth routing ----------

  private async handleAuthSuccess(response: any): Promise<void> {
    let parsed: AuthResponseDto;

    if (response instanceof Blob) {
      try {
        parsed = JSON.parse(await response.text());
      } catch (e) {
        console.error('Failed to parse Blob response:', e);
        return;
      }
    } else {
      parsed = response;
    }

    if (!parsed || !parsed.token) {
      console.error('No token in response:', parsed);
      return;
    }

    // 1) Persist
    localStorage.setItem(this.TOKEN_KEY, parsed.token);

    const userData = {
      roles: parsed.roles ?? [],
      userId: parsed.userId,
      username: parsed.username,
      email: parsed.email
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(userData));

    // 2) Reactive state
    this.isAuthenticatedSubject.next(true);
    this.currentUserSubject.next(userData);

    // 3) Resolve landing mode
    const hasClient = userData.roles.includes('ROLE_CLIENT');
    const hasWorker = userData.roles.includes('ROLE_WORKER');
    const storedMode = this.getPreferredMode();

    let mode: 'client' | 'worker' = 'client';
    if (storedMode === 'worker' && hasWorker) mode = 'worker';
    else if (!hasClient && hasWorker) mode = 'worker';

    localStorage.setItem(this.MODE_KEY, mode);

    // 4) Navigate; NavigationEnd will refresh contextRole
    const target = mode === 'worker' ? '/worker/worker-dashboard' : '/client/home';
    this.router.navigateByUrl(target);
  }
}
