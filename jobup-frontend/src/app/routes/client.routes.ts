import { Routes } from '@angular/router';
import { clientGuard } from '../guards/client.guard';

export const CLIENT_ROUTES: Routes = [
  {
    path: '',
    canActivate: [clientGuard],
    children: [
      {
        path: 'home',
        loadComponent: () => import('../pages/home/home.component').then(m => m.HomeComponent),
        title: 'Client Dashboard'
      },
      {
        path: 'workers',
        loadComponent: () => import('../pages/worker-list/worker-list.component').then(m => m.WorkerListComponent),
        title: 'Browse Workers'
      },
      {
        path: 'workers/:id',
        loadComponent: () => import('../pages/worker-detail/worker-detail.component').then(m => m.WorkerDetailComponent),
        title: 'Worker Details'
      },
      {
        path: 'become-worker',
        loadComponent: () => import('../pages/become-worker/become-worker.component').then(m => m.BecomeWorkerComponent),
        title: 'become Worker'
      },
      {
        path: 'posts/create',
        loadComponent: () => import('../pages/post-create/post-create.component').then(m => m.PostCreateComponent),
        title: 'Create Job Post'
      },
      {
        path: 'my-posts',
        loadComponent: () => import('../pages/my-posts/my-posts.component').then(m => m.MyPostsComponent),
        title: 'My Posts'
      },
      {
        path: 'notifications',
        loadComponent: () => import('../pages/notifications/notifications.component').then(m => m.NotificationsComponent),
        title: 'Notifications'
      },
      // Add more client pages here...
      {
        path: '',
        redirectTo: 'home',
        pathMatch: 'full'
      },
      // NEW: unknown inside /client → /client/home
      { path: '**', redirectTo: 'home' }
    ]
  }
]; 