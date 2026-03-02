import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { NavigationDashboardService } from '../../services/navigation-dashboard.service';
import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, HeaderComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  private authService = inject(AuthService);
  private dashboardService = inject(NavigationDashboardService);
  private router = inject(Router);

  currentUser$ = this.authService.currentUser$;
  currentDashboard$ = this.dashboardService.getCurrentRole();
  currentDashboard: 'client' | 'worker' = 'client';

  ngOnInit(): void {
    this.dashboardService.getCurrentRole().subscribe(dashboard => {
      this.currentDashboard = dashboard;
    });
  }

  switchDashboard(): void {
    if (this.currentDashboard === 'client') {
      this.dashboardService.switchToWorkerMode();
    } else {
      this.dashboardService.switchToClientMode();
    }
  }

  isWorker(): boolean {
    return this.authService.isWorker();
  }

    logout(): void {
    this.authService.logout();
  }

  switchToWorkerMode(): void {
    this.dashboardService.switchToWorkerMode();
  }

  // Add navigation methods for client pages
  goToBookings(): void {
    this.dashboardService.navigateToClientPage('bookings');
  }

  goToProfile(): void {
    this.dashboardService.navigateToClientPage('profile');
  }

  goToFavorites(): void {
    this.dashboardService.navigateToClientPage('favorites');
  }
  goTolist(): void {
    this.dashboardService.navigateToClientPage('worker-list');
  }

  goToMyPosts(): void {
    this.dashboardService.navigateToClientPage('my-posts');
  }
}
