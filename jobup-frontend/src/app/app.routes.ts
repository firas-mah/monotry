import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { redirectGuard } from './guards/redirect.guard';
import { CLIENT_ROUTES } from './routes/client.routes';
import { WORKER_ROUTES } from './routes/worker.routes';

export const routes: Routes = [
  // Authentication routes (public)
  {
    path: 'sign-in',
    loadComponent: () => import('./pages/sign-in/sign-in.component').then(m => m.SignInComponent),
    canActivate: [redirectGuard]
  },
  {
    path: 'sign-up',
    loadComponent: () => import('./pages/sign-up/sign-up.component').then(m => m.SignUpComponent),
    canActivate: [redirectGuard]
  },
  {
    path: 'profile-edit',
    loadComponent: () => import('./pages/profile-edit/profile-edit.component').then(m => m.ProfileEditComponent),
    title: 'Edit Profile',
    canActivate: [authGuard]
  },
  {
    path: 'change-password',
    loadComponent: () => import('./pages/change-password/change-password.component').then(m => m.ChangePasswordComponent),
    title: 'Change Password',
    canActivate: [authGuard]
  },
  {
    path: 'notifications',
    loadComponent: () => import('./pages/notifications/notifications.component').then(m => m.NotificationsComponent),
    title: 'Notifications',
    canActivate: [authGuard]
  },

  // Client routes (protected by clientGuard)
  {
    path: 'client',
    children: CLIENT_ROUTES
  },

  // Worker routes (protected by workerGuard)
  {
    path: 'worker',
    children: WORKER_ROUTES
  },

  // Legacy routes for backward compatibility
  {
    path: 'home',
    redirectTo: '/client/home',
    pathMatch: 'full'
  },
  {
    path: 'worker-dashboard',
    redirectTo: '/worker/worker-dashboard',
    pathMatch: 'full'
  },

  // Default redirects
  {
    path: '',
    redirectTo: '/sign-in',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/sign-in'
  }
];
