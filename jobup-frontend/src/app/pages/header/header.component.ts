import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { CommonModule } from '@angular/common';
import { ProfileDropdownComponent } from '../profile-dropdown/profile-dropdown.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, ProfileDropdownComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);

  currentUser$ = this.authService.currentUser$;
  unreadCount$ = this.notificationService.unreadCount$;

  logout(): void {
    this.authService.logout();
  }

  goToNotifications(): void {
    this.router.navigate(['/notifications']);
  }
}
