// auth.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Limit interception to your API base only
  // e.g. environment.apiBaseUrl = 'http://localhost:8083/JobUp'
  const apiBase = environment.apiBaseUrl?.replace(/\/+$/, ''); // trim trailing slash
  const isApiCall =
    !!apiBase && (req.url.startsWith(apiBase) || req.url.startsWith('/api') || req.url.startsWith('/JobUp'));

  // Never attach Authorization to third-party URLs
  if (!isApiCall) {
    return next(req);
  }

  // Skip only login/register
  const isLoginOrRegister =
    req.url.includes('/auth/login') || req.url.includes('/auth/register');

  if (isLoginOrRegister || !token) {
    // login/register (or no token yet) -> send as-is
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });

  // Optional debug
  console.log('Interceptor - Request URL:', req.url, 'Has token:', !!token);

  return next(authReq);
};
