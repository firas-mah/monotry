import { Component, inject, OnInit } from '@angular/core';
import { NavigationDashboardService } from '../../services/navigation-dashboard.service';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HeaderComponent } from '../header/header.component';
import { SmartChatComponent } from '../smart-chat/smart-chat.component';
import { ProposalControllerService } from '../../generated-sources/openapi';

@Component({
  selector: 'app-worker-dashboard',
  standalone: true,
  imports: [CommonModule, HeaderComponent, SmartChatComponent],
  templateUrl: './worker-dashboard.component.html',
  styleUrl: './worker-dashboard.component.css'
})
export class WorkerDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private dashboardService = inject(NavigationDashboardService);
  private router = inject(Router);
  private proposalService = inject(ProposalControllerService);

  currentUser$ = this.authService.currentUser$;
  currentDashboard$ = this.dashboardService.getCurrentRole();
  currentDashboard: 'client' | 'worker' = 'client';

  // Chat properties
  showChat = false;
  selectedClientId: string = '';
  selectedClientName: string = '';
  currentUserId: string = '';
  proposals: any[] = [];
  isLoadingProposals = false;
  currentUserName = '';

  constructor() {
    this.currentDashboard$.subscribe(dashboard => {
      this.currentDashboard = dashboard;
    });
  }
  
  ngOnInit(): void {
    // Set to worker mode when on worker dashboard
    this.dashboardService.switchToWorkerMode();
    this.currentDashboard = 'worker';
    this.currentUserId = this.authService.getCurrentUserId() || '';
    this.currentUserName = this.authService.getCurrentUser()?.username || '';
    this.loadWorkerProposals();
  }

  loadWorkerProposals(): void {
    this.isLoadingProposals = true;
    
    // Get the current worker's ID
    const workerId = this.authService.getCurrentUserId();
    
    if (!workerId) {
      console.error('No worker ID found');
      this.proposals = [];
      this.isLoadingProposals = false;
      return;
    }
    
    console.log('Loading proposals for worker ID:', workerId);
    
    // Use the new endpoint to get proposals for this worker
    this.proposalService.getProposalsByWorkerId(workerId).subscribe({
      next: (response: any) => {
        console.log('Worker proposals response:', response);
        console.log('Type of response:', typeof response);
        
        let proposals: any[] = [];
        
        // Handle Blob response
        if (response instanceof Blob) {
          console.log('Response is Blob, converting...');
          const reader = new FileReader();
          reader.onload = () => {
            try {
              const jsonData = JSON.parse(reader.result as string);
              console.log('Parsed JSON from Blob:', jsonData);
              proposals = Array.isArray(jsonData) ? jsonData : [];
            } catch (error) {
              console.error('Error parsing Blob:', error);
              proposals = [];
            }
            this.proposals = proposals;
            this.isLoadingProposals = false;
          };
          reader.readAsText(response);
        } else {
          // Handle direct JSON response
          proposals = Array.isArray(response) ? response : [];
          this.proposals = proposals;
          this.isLoadingProposals = false;
        }
      },
      error: (error: any) => {
        console.error('Error loading worker proposals:', error);
        this.proposals = [];
        this.isLoadingProposals = false;
      }
    });
  }

  openChatWithClient(clientId: string, clientName: string): void {
    console.log('Opening chat with client:', { clientId, clientName });
    this.selectedClientId = clientId;
    this.selectedClientName = clientName;
    this.showChat = true;
  }

  closeChat(): void {
    this.showChat = false;
    this.selectedClientId = '';
    this.selectedClientName = '';
  }

  switchDashboard(): void {
    if (this.currentDashboard === 'worker') {
      this.dashboardService.switchToClientMode();
      this.router.navigate(['/home']);
    } else {
      this.dashboardService.switchToWorkerMode();
      this.router.navigate(['/worker-dashboard']);
    }
  }

  openChatFromProposal(p: any): void {
    // You (worker) might be the sender or the receiver depending on who initiated.
    const me = this.currentUserId;

    // If I sent the proposal, the client is the receiver; else the client is the sender.
    const clientId   = p.senderId === me ? (p.receiverId || '') : (p.senderId || '');
    const clientName = p.senderId === me ? (p.receiverName || 'Client') : (p.senderName || 'Client');

    if (!clientId) {
      console.warn('No client id found on proposal:', p);
      return;
    }

    this.openChatWithClient(clientId, clientName);
  }

  isWorker(): boolean {
    return this.authService.isWorker();
  }

  logout(): void {
    this.authService.logout();
  }

  getCurrentRole(): string {
    return this.currentDashboard === 'worker' ? 'Worker' : 'Client';
  }

  switchToClientMode(): void {
    this.dashboardService.switchToClientMode();
  }

  // Add navigation methods for worker pages
  goToProfile(): void {
    this.dashboardService.navigateToWorkerPage('profile');
  }

  goToJobs(): void {
    this.dashboardService.navigateToWorkerPage('jobs');
  }

  goToSchedule(): void {
    this.dashboardService.navigateToWorkerPage('schedule');
  }

  goToEarnings(): void {
    this.dashboardService.navigateToWorkerPage('earnings');
  }

  goToPosts(): void {
    this.dashboardService.navigateToWorkerPage('posts');
  }

  goToSavedPosts(): void {
    this.dashboardService.navigateToWorkerPage('saved-posts');
  }

  getProposalStatusBadge(status: string): string {
    switch (status) {
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'ACCEPTED': return 'bg-green-100 text-green-800';
      case 'DECLINED': return 'bg-red-100 text-red-800';
      case 'NEGOTIATING': return 'bg-blue-100 text-blue-800';
      case 'COMPLETED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }
}
