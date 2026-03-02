import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';
import { NotificationDto } from '../../generated-sources/openapi';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.css'
})
export class NotificationsComponent implements OnInit {
  notifications: NotificationDto[] = [];
  isLoading = true;

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
    this.notificationService.requestNotificationPermission();
  }

  loadNotifications(): void {
    this.isLoading = true;
    this.notificationService.notifications$.subscribe({
      next: (notifications) => {
        this.notifications = notifications;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading notifications:', error);
        this.isLoading = false;
      }
    });
  }

  markAsRead(notification: NotificationDto): void {
    if (!notification.read) {
      this.notificationService.markAsRead(notification.id!).subscribe({
        next: () => {
          notification.read = true;
          this.notificationService.loadUnreadCount();
        },
        error: (error) => {
          console.error('Error marking notification as read:', error);
        }
      });
    }
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
        this.notificationService.loadUnreadCount();
      },
      error: (error) => {
        console.error('Error marking all notifications as read:', error);
      }
    });
  }

  deleteNotification(notification: NotificationDto): void {
    this.notificationService.deleteNotification(notification.id!).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.id !== notification.id);
        this.notificationService.loadUnreadCount();
      },
      error: (error) => {
        console.error('Error deleting notification:', error);
      }
    });
  }

  navigateToAction(notification: NotificationDto): void {
    this.markAsRead(notification);
    if (notification.actionUrl) {
      this.router.navigate([notification.actionUrl]);
    }
  }

  getNotificationIcon(type: string | undefined): string {
    return this.notificationService.getNotificationIcon(type || '');
  }

  getNotificationColor(type: string | undefined): string {
    return this.notificationService.getNotificationColor(type || '');
  }

  trackByNotificationId(index: number, notification: NotificationDto): string {
    return notification.id || index.toString();
  }

  goBack(): void {
    this.router.navigate(['/']);
  }
}
