import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import {
  ChatControllerService,
  ProposalControllerService,
  ChatMessageDto,
  JobProposalDto,
  JobDealControllerService
} from '../../generated-sources/openapi';
import { Subscription, interval } from 'rxjs';
import { WebSocketService } from '../../services/websocket.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-smart-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './smart-chat.component.html',
  styleUrls: ['./smart-chat.component.css']
})
export class SmartChatComponent implements OnInit, OnDestroy {
  @Input() workerId!: string;
  @Input() workerName!: string;
  @Output() closeChatEvent = new EventEmitter<void>();
  @Input() clientId?: string;
  @Input() clientName?: string;
  @Input() forceUserType?: 'CLIENT' | 'WORKER';


  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  messages: ChatMessageDto[] = [];
  proposals: JobProposalDto[] = [];
  deals: any[] = [];
  newMessage: string = '';
  isLoading = false;
  showProposalForm = false;
  showRatingForm = false;
  selectedDealId: string = '';
  proposalForm = {
    title: '',
    description: '',
    duration: 1,
    price: 0,
    location: '',
    scheduledTime: ''
  };
  ratingForm = {
    rating: 5,
    review: ''
  };
  ratingError: string = '';
  isSubmittingRating = false;

  private currentUserId: string = '';
  private refreshSubscription?: Subscription;
  private websocketSubscription?: Subscription;
  private connectionStatusSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    private chatService: ChatControllerService,
    private proposalService: ProposalControllerService,
    private dealService: JobDealControllerService,
    private websocketService: WebSocketService,
    private http: HttpClient  
  ) {}

  private canRateDeal$(dealId: string) {
    const url = `${environment.apiBaseUrl}/api/deals/${dealId}/can-rate`;
    return this.http.get<boolean>(url);
  }

  private addRating$(dealId: string, body: {rating: number; review: string}) {
    return this.http.post(`${environment.apiBaseUrl}/api/deals/${dealId}/rating`, body);
  }

  currentUserType: 'CLIENT' | 'WORKER' = 'CLIENT';

  ngOnInit(): void {
    // Always use AuthService helpers so we don't depend on raw object shapes
    this.currentUserId = this.authService.getCurrentUserId() || '';
    this.currentUserType = this.forceUserType ?? this.authService.getBusinessRole();

    console.log('Smart Chat initialized with:', {
      workerId: this.workerId,
      workerName: this.workerName,
      clientId: this.clientId,
      currentUserId: this.currentUserId,
      currentUserType: this.currentUserType
    });

    // If we cannot compute a chatId (e.g., worker view without a clientId), just stop here.
    const chatId = this.generateChatId();
    if (!chatId) {
      console.warn('Chat not initializable: missing client/worker id.');
      return;
    }

    this.loadMessages();
    this.loadProposals();
    this.loadDeals();
    this.initializeWebSocket();

  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
    if (this.websocketSubscription) {
      this.websocketSubscription.unsubscribe();
    }
    if (this.connectionStatusSubscription) {
      this.connectionStatusSubscription.unsubscribe();
    }
    this.websocketService.disconnect();
  }

  loadMessages(): void {
    const chatId = this.generateChatId();
    if (!chatId) return;
    this.isLoading = true;
    console.log('Loading messages for chatId:', chatId);

    this.chatService.getChatMessages(chatId).subscribe({
      next: (res) => {
        this.messages = res || [];
        this.scrollToBottom();
        this.isLoading = false;
      },
      error: (e) => { console.error(e); this.isLoading = false; }
    });
  }

  loadProposals(): void {
    const chatId = this.generateChatId();
    
    this.proposalService.getProposalsByChatId(chatId).subscribe({
      next: (response: any) => {
        console.log('Proposals loaded:', response);
        
        // Handle Blob response
        if (response instanceof Blob) {
          const reader = new FileReader();
          reader.onload = () => {
            try {
              const jsonResponse = JSON.parse(reader.result as string);
              this.proposals = jsonResponse || [];
            } catch (error) {
              console.error('Error parsing Blob response:', error);
              this.proposals = [];
            }
          };
          reader.readAsText(response);
        } else {
          this.proposals = response || [];
        }
      },
      error: (error: any) => {
        console.error('Error loading proposals:', error);
        this.proposals = [];
      }
    });
  }

  loadDeals(): void {
    const chatId = this.generateChatId();
    
    this.dealService.getDealsByChatId(chatId).subscribe({
      next: (response: any) => {
        console.log('Deals loaded:', response);
        
        // Handle Blob response
        if (response instanceof Blob) {
          const reader = new FileReader();
          reader.onload = () => {
            try {
              const jsonResponse = JSON.parse(reader.result as string);
              this.deals = jsonResponse || [];
            } catch (error) {
              console.error('Error parsing Blob response:', error);
              this.deals = [];
            }
          };
          reader.readAsText(response);
        } else {
          this.deals = response || [];
        }
      },
      error: (error: any) => {
        console.error('Error loading deals:', error);
        this.deals = [];
      }
    });
  }

  sendMessage(): void {
    if (!this.newMessage.trim()) return;

    const chatId = this.generateChatId();
    const messageContent = this.newMessage.trim();
    
    // Determine receiver information based on current user type
    let receiverId: string;
    let receiverName: string;
    let receiverType: string;
    
    if (this.currentUserType === 'CLIENT') {
      // If current user is CLIENT, receiver is the WORKER they're contacting
      receiverId = this.workerId;
      receiverName = this.workerName;
      receiverType = 'WORKER';
    } else {
      // If current user is WORKER, receiver is the CLIENT who contacted them
      receiverId = this.clientId || '';
      receiverName = this.clientName || 'Client';
      receiverType = 'CLIENT';
    }
    
    // Create WebSocket message
    const wsMessage: any = {
      chatId: chatId,
      senderId: this.currentUserId,
      senderName: this.authService.getCurrentUser()?.username || '',
      senderType: this.currentUserType,
      receiverId: receiverId,
      receiverName: receiverName,
      receiverType: receiverType,
      content: messageContent
    };

    console.log('Sending message via WebSocket:', wsMessage);

    // Send via WebSocket ONLY
    this.websocketService.sendMessage(chatId, wsMessage);
    
    this.newMessage = '';
  }

  sendProposal(): void {
    if (!this.proposalForm.title || !this.proposalForm.description || this.proposalForm.price <= 0) {
      alert('Please fill in all required fields');
      return;
    }

    const chatId = this.generateChatId();
    
    // Determine receiver information based on current user type
    let receiverId: string;
    let receiverName: string;
    let receiverType: string;
    
    if (this.currentUserType === 'CLIENT') {
      receiverId = this.workerId;
      receiverName = this.workerName;
      receiverType = 'WORKER';
    } else {
      receiverId = this.clientId || '';
      receiverName = this.clientName || 'Client';
      receiverType = 'CLIENT';
    }

    const proposal: any = {
      chatId,
      senderId: this.currentUserId,
      senderName: this.authService.getCurrentUser()?.username || '',
      senderType: this.currentUserType,   // <-- FIXED (no hard-code)
      receiverId,
      receiverName,
      receiverType,
      title: this.proposalForm.title,
      description: this.proposalForm.description,
      durationMinutes: this.proposalForm.duration,
      price: this.proposalForm.price,
      location: this.proposalForm.location,
      scheduledAt: this.proposalForm.scheduledTime || null 
    };

    console.log('Sending proposal via WebSocket:', proposal);

    // Send via WebSocket ONLY
    this.websocketService.sendProposal(chatId, proposal);
    
    // Clear form and reload proposals
    this.showProposalForm = false;
    this.resetProposalForm();
    
    // Reload proposals after a short delay
    setTimeout(() => {
      this.loadProposals();
    }, 500);
  }

  respondToProposal(proposalId: string, response: 'ACCEPTED' | 'DECLINED' | 'NEGOTIATED'): void {
    const updateStatusRequest = {
      status: response
    };
    
    this.proposalService.updateProposalStatus(proposalId, updateStatusRequest).subscribe({
      next: (response: any) => {
        console.log('Proposal response sent successfully:', response);
        
        // If accepted, create a deal
        if (response.status === 'ACCEPTED') {
          this.createDealFromProposal(proposalId);
        }
        
        this.loadProposals();
      },
      error: (error: any) => {
        console.error('Error sending proposal response:', error);
      }
    });
  }

  createDealFromProposal(proposalId: string): void {
    this.dealService.createDealFromProposal(proposalId).subscribe({
      next: (response) => {
        console.log('Deal created successfully:', response);
        this.loadDeals();
        // Send a confirmation message
        this.sendDealConfirmationMessage(response);
      },
      error: (error) => {
        console.error('Error creating deal:', error);
      }
    });
  }

  updateDealStatus(dealId: string, status: 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'): void {
    const updateStatusRequest = { 
      status: status as any // Use any type to bypass enum issues
    };
    
    this.dealService.updateDealStatus(dealId, updateStatusRequest).subscribe({
      next: (response) => {
        console.log('Deal status updated successfully:', response);
        this.loadDeals();
      },
      error: (error) => {
        console.error('Error updating deal status:', error);
      }
    });
  }

  selectDealForRating(dealId: string): void {
    this.canRateDeal$(dealId).subscribe({
      next: (canRate: boolean) => {
        if (canRate) {
          this.selectedDealId = dealId;
          this.ratingError = '';
          this.showRatingForm = true;
        } else {
          alert('This deal cannot be rated. It may already be rated or not completed yet.');
        }
      },
      error: (error: any) => {
        console.error('Error checking if deal can be rated:', error);
        alert('Unable to check rating status. Please try again.');
      }
    });
  }

  addRating(): void {
    if (!this.selectedDealId) return;

    // ...validation...

    this.isSubmittingRating = true;
    const ratingRequest = { rating: this.ratingForm.rating, review: this.ratingForm.review.trim() };

    this.addRating$(this.selectedDealId, ratingRequest).subscribe({
      next: (response: any) => {
        console.log('Rating added successfully:', response);
        this.showRatingForm = false;
        this.resetRatingForm();
        this.selectedDealId = '';
        this.isSubmittingRating = false;
        this.loadDeals();
        alert('Rating submitted successfully!');
      },
      error: (error: any) => {
        console.error('Error adding rating:', error);
        this.isSubmittingRating = false;
        if (error.status === 400) this.ratingError = error.error?.message || 'Invalid rating data. Please check your input.';
        else if (error.status === 409) this.ratingError = 'This deal has already been rated.';
        else if (error.status === 403) this.ratingError = 'You are not authorized to rate this deal.';
        else this.ratingError = 'Failed to submit rating. Please try again.';
      }
    });
  }

  private generateChatId(): string {
    let clientId = '';
    let workerId = '';

    if (this.currentUserType === 'CLIENT') {
      clientId = this.currentUserId;
      workerId = this.workerId;
    } else { // ROLE_WORKER
      clientId = this.clientId || '';
      workerId = this.currentUserId;
    }

    if (!clientId || !workerId) {
      console.warn('Missing clientId/workerId for chat.');
      return '';
    }

    return clientId < workerId ? `${clientId}_${workerId}` : `${workerId}_${clientId}`;
  }

  private resetProposalForm(): void {
    this.proposalForm = {
      title: '',
      description: '',
      duration: 1,
      price: 0,
      location: '',
      scheduledTime: ''
    };
  }

  private resetRatingForm(): void {
    this.ratingForm = {
      rating: 5,
      review: ''
    };
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }

  closeChat(): void {
    this.closeChatEvent.emit();
  }

  toggleProposalForm(): void {
    this.showProposalForm = !this.showProposalForm;
  }

  isMessageFromCurrentUser(message: any): boolean { // Changed to any to avoid ChatMessageDto import
    return message.senderName === this.authService.getCurrentUser()?.username;
  }

  isProposalMessage(message: any): boolean { // Changed to any to avoid ChatMessageDto import
    return message.messageType === 'PROPOSAL';
  }

  isProposalResponse(message: any): boolean { // Changed to any to avoid ChatMessageDto import
    return message.messageType === 'PROPOSAL_RESPONSE';
  }

  getStarRating(rating: number | undefined): string[] {
    const stars = [];
    const ratingValue = rating || 0;

    for (let i = 1; i <= 5; i++) {
      if (i <= ratingValue) {
        stars.push('★');
      } else {
        stars.push('☆');
      }
    }
    return stars;
  }

  private sendDealConfirmationMessage(deal: any): void {
    const chatId = this.generateChatId();
    const messageContent = `✅ Job Deal Confirmed: ${deal.title} - ${deal.duration}h - ${deal.price} TND`;
    
    const wsMessage: any = { // Changed to any to avoid ChatMessageDto import
      chatId: chatId,
      senderId: this.currentUserId,
      senderName: 'System',
      senderType: 'SYSTEM',
      content: messageContent
    };

    this.websocketService.sendMessage(chatId, wsMessage);
  }

  private initializeWebSocket(): void {
    const chatId = this.generateChatId();
    const username = this.authService.getCurrentUser()?.username || '';
    
    // Connect to WebSocket
    this.websocketService.connectToChat(chatId, username, this.currentUserId, this.currentUserType);

    // Subscribe to incoming messages
    this.websocketSubscription = this.websocketService.message$.subscribe(message => {
      if (message) {
        this.messages.push(message);
        this.scrollToBottom();
      }
    });
    this.websocketService.proposal$.subscribe(proposal => {
      if (proposal) {
        this.proposals.push(proposal);
      }
    });

    // Subscribe to connection status
    this.connectionStatusSubscription = this.websocketService.connectionStatus$.subscribe(isConnected => {
      console.log('WebSocket connection status:', isConnected);
    });
  }
}
