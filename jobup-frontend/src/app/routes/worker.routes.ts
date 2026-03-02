import { Routes } from '@angular/router';
import { workerGuard } from '../guards/worker.guard';

export const WORKER_ROUTES: Routes = [
  {
    path: '',
    canActivate: [workerGuard],
    children: [
      {
        path: 'worker-dashboard',
        loadComponent: () => import('../pages/worker-dashboard/worker-dashboard.component').then(m => m.WorkerDashboardComponent),
        title: 'Worker Dashboard'
      },
      {
        path: 'posts',
        loadComponent: () => import('../pages/post-list/post-list.component').then(m => m.PostListComponent),
        title: 'Job Posts'
      },
      {
        path: 'saved-posts',
        loadComponent: () => import('../pages/saved-posts/saved-posts.component').then(m => m.SavedPostsComponent),
        title: 'Saved Posts'
      },
      {
        path: 'notifications',
        loadComponent: () => import('../pages/notifications/notifications.component').then(m => m.NotificationsComponent),
        title: 'Notifications'
      },
      // Add more worker pages here...
      {
        path: '',
        redirectTo: 'worker-dashboard',
        pathMatch: 'full'
      },
       // NEW: unknown inside /worker → /worker/worker-dashboard
      { path: '**', redirectTo: 'worker-dashboard' }
    ]
  }
]; 