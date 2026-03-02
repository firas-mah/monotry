import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { ChatMessageDto, JobProposalDto, NotificationDto } from '../generated-sources/openapi';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client: Client | null = null;

  private messageSubject = new BehaviorSubject<ChatMessageDto | null>(null);
  private proposalSubject = new BehaviorSubject<JobProposalDto | null>(null);
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);

  // Optional: expose notification streams if you want to use them directly
  private notificationSubject = new BehaviorSubject<NotificationDto | null>(null);
  private notificationCountSubject = new BehaviorSubject<number>(0);

  public message$ = this.messageSubject.asObservable();
  public proposal$ = this.proposalSubject.asObservable();
  public connectionStatus$ = this.connectionStatusSubject.asObservable();
  public notification$ = this.notificationSubject.asObservable();
  public notificationCount$ = this.notificationCountSubject.asObservable();
  private subscribedTopics = new Set<string>();

  constructor(private authService: AuthService) {}

 /** Global connection for the whole app (notifications + optional chat). */
  connectIfNeeded(): void {
    if (this.client?.connected) return;

    const token = this.authService.getToken();
    const wsUrl = `${environment.apiBaseUrl}/ws`; // ex: http://localhost:8083/JobUp/ws

    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      debug: (str) => console.log('STOMP:', str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    this.client.onConnect = () => {
      this.connectionStatusSubject.next(true);

      // PERSONAL QUEUES (note: no userId in path)
      this.client!.subscribe('/user/queue/notifications', (msg: IMessage) => {
        const n = JSON.parse(msg.body);
        this.notificationSubject.next(n);
      });

      this.client!.subscribe('/user/queue/notification-count', (msg: IMessage) => {
        const count = JSON.parse(msg.body);
        this.notificationCountSubject.next(count);
      });
    };

    this.client.onDisconnect = () => this.connectionStatusSubject.next(false);
    this.client.onStompError = () => this.connectionStatusSubject.next(false);

    this.client.activate();
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.connectionStatusSubject.next(false);
  }

    /** --- Chat helpers (unchanged) --- */
  connectToChat(chatId: string, username: string, userId: string, userType: string): void {
    this.connectIfNeeded();
    const topic = `/topic/chat/${chatId}`;

    const trySub = () => {
      if (!this.client?.connected) return;
      if (!this.subscribedTopics.has(topic)) {
        this.client.subscribe(topic, (message) => {
          try {
            const chatMessage = JSON.parse(message.body);
            this.messageSubject.next(chatMessage);
          } catch (e) {
            console.error('Parse chat message error', e);
          }
        });
        this.subscribedTopics.add(topic);
        this.sendJoinMessage(chatId, username, userId, userType);
      }
    };

    if (this.client?.connected) trySub();
    else this.connectionStatus$.subscribe((ok) => ok && trySub());
  }


  sendMessage(chatId: string, message: ChatMessageDto): void {
    if (!this.client?.connected) return console.error('WS not connected');
    this.client.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify({
        chatId,
        senderId: message.senderId || '',
        senderName: message.senderName || '',
        senderType: message.senderType || '',
        receiverId: (message as any).receiverId || '',
        receiverName: (message as any).receiverName || '',
        receiverType: (message as any).receiverType || '',
        content: message.content || ''
      })
    });
  }

  sendProposal(chatId: string, proposal: any): void {
    if (!this.client?.connected) return console.error('WS not connected');
    this.client.publish({
      destination: '/app/chat.sendProposal',
      body: JSON.stringify({
        chatId,
        senderId: proposal.senderId || '',
        senderName: proposal.senderName || '',
        senderType: proposal.senderType || '',
        receiverId: proposal.receiverId || '',
        receiverName: proposal.receiverName || '',
        receiverType: proposal.receiverType || '',
        title: proposal.title || '',
        description: proposal.description || '',
        // send what WS backend expects, using either shape:
        duration: (proposal.duration ?? proposal.durationMinutes ?? 0),
        price: typeof proposal.price === 'number' ? proposal.price : (proposal.price ?? 0),
        location: proposal.location || '',
        scheduledTime: (proposal.scheduledTime ?? proposal.scheduledAt ?? '')
      })
    });
  }

  private sendJoinMessage(chatId: string, username: string, userId: string, userType: string): void {
    if (!this.client?.connected) return;
    this.client.publish({
      destination: '/app/chat.addUser',
      body: JSON.stringify({
        type: 'JOIN',
        chatId,
        senderId: userId,
        senderName: username,
        senderType: userType,
        receiverId: '',
        receiverName: '',
        receiverType: '',
        content: `${username} joined the chat`
      })
    });
  }

  isConnected(): boolean {
    return !!this.client?.connected;
  }

  // Notification-specific methods
    /** Legacy public subscribe helpers kept for compatibility – now ignore userId */
  subscribeToUserNotifications(callback: (n: NotificationDto) => void): void {
    this.connectIfNeeded();
    const sub = () =>
      this.client?.subscribe('/user/queue/notifications', (m) => callback(JSON.parse(m.body)));
    if (this.client?.connected) sub();
    else this.connectionStatus$.subscribe((ok) => ok && sub());
  }

  subscribeToNotificationCount(callback: (count: number) => void): void {
    this.connectIfNeeded();
    const sub = () =>
      this.client?.subscribe('/user/queue/notification-count', (m) => callback(JSON.parse(m.body)));
    if (this.client?.connected) sub();
    else this.connectionStatus$.subscribe((ok) => ok && sub());
  }
}
