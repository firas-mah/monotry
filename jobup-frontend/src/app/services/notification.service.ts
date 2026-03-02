import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { WebSocketService } from './websocket.service';
import { NotificationControllerService, NotificationDto } from '../generated-sources/openapi';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<NotificationDto[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);

  public notifications$ = this.notificationsSubject.asObservable();
  public unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(
    private notificationController: NotificationControllerService,
    private authService: AuthService,
    private webSocketService: WebSocketService
  ) {
    this.initializeNotifications();
  }

  private initializeNotifications(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser?.id) {                 // <-- was userId
      this.loadNotifications();
      this.loadUnreadCount();
      this.webSocketService.connectIfNeeded();
      this.webSocketService.connectionStatus$.subscribe(connected => {
        if (!connected) return;
        this.webSocketService.subscribeToUserNotifications((notification) => {
          this.notificationsSubject.next([notification, ...this.notificationsSubject.value]);
          this.loadUnreadCount();
          this.showBrowserNotification(notification);
        });
        this.webSocketService.subscribeToNotificationCount((count) => {
          this.unreadCountSubject.next(count);
        });
      });
    }
  }

  private setupWebSocketListeners(userId: string): void {
    // Listen for real-time notifications
    this.webSocketService.subscribeToUserNotifications( (notification: NotificationDto) => {
      console.log('Received real-time notification:', notification);
      
      // Add to notifications list
      const currentNotifications = this.notificationsSubject.value;
      this.notificationsSubject.next([notification, ...currentNotifications]);
      
      // Update unread count
      this.loadUnreadCount();
      
      // Show browser notification if supported
      this.showBrowserNotification(notification);
    });

    // Listen for notification count updates
    this.webSocketService.subscribeToNotificationCount((count: number) => {
      console.log('Received notification count update:', count);
      this.unreadCountSubject.next(count);
    });
  }

  loadNotifications(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) return;                 // <-- was userId

    this.notificationController.getNotificationsByUserId(currentUser.id)
      .subscribe({
        next: (notifications) => this.notificationsSubject.next(notifications),
        error: (err) => { console.error(err); this.notificationsSubject.next([]); }
      });
}

  loadUnreadCount(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) return;                 // <-- was userId

    this.notificationController.getUnreadNotificationCount(currentUser.id)
      .subscribe({
        next: (count) => this.unreadCountSubject.next(typeof count === 'number' ? count : parseInt(String(count), 10) || 0),
        error: (err) => { console.error(err); this.unreadCountSubject.next(0); }
      });
  }

  markAsRead(notificationId: string): Observable<NotificationDto> {
    return this.notificationController.markAsRead(notificationId);
  }

  markAllAsRead(): Observable<any> {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser?.id) {
      throw new Error('User not authenticated');
    }
    return this.notificationController.markAllAsRead(currentUser.id);
  }

  deleteNotification(notificationId: string): Observable<any> {
    return this.notificationController.deleteNotification(notificationId);
  }

  private showBrowserNotification(notification: NotificationDto): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(notification.senderName || 'JobUp Notification', {
        body: notification.message || 'You have a new notification',
        icon: '/assets/icons/notification-icon.png',
        badge: '/assets/icons/notification-badge.png'
      });
    }
  }

  requestNotificationPermission(): void {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission().then(permission => {
        console.log('Notification permission:', permission);
      });
    }
  }

  getNotificationIcon(type: string | undefined): string {
    switch (type) {
      case 'POST_LIKED':
        return '👍';
      case 'POST_COMMENTED':
        return '💬';
      case 'POST_SAVED':
        return '📌';
      case 'PROPOSAL_RECEIVED':
        return '📋';
      case 'PROPOSAL_ACCEPTED':
        return '✅';
      case 'PROPOSAL_DECLINED':
        return '❌';
      default:
        return '🔔';
    }
  }

  getNotificationColor(type: string | undefined): string {
    switch (type) {
      case 'POST_LIKED':
        return 'text-red-600';
      case 'POST_COMMENTED':
        return 'text-blue-600';
      case 'POST_SAVED':
        return 'text-yellow-600';
      case 'PROPOSAL_RECEIVED':
        return 'text-green-600';
      case 'PROPOSAL_ACCEPTED':
        return 'text-green-600';
      case 'PROPOSAL_DECLINED':
        return 'text-red-600';
      default:
        return 'text-gray-600';
    }
  }
}


