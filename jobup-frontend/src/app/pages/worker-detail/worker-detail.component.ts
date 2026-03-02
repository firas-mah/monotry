import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkerControllerService, WorkerResponseDto, JobDealControllerService } from '../../generated-sources/openapi';
import { AuthService } from '../../services/auth.service';
import { SmartChatComponent } from '../smart-chat/smart-chat.component';
import { HeaderComponent } from '../header/header.component';
import { ProfilePictureDisplayComponent } from '../profile-picture-display/profile-picture-display.component';
import { RatingDisplayComponent } from '../rating-display/rating-display.component';
import { RatingStatsComponent } from '../rating-stats/rating-stats.component';
import { WorkerRatingStatsDto } from '../../generated-sources/openapi/model/workerRatingStatsDto';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-worker-detail',
  standalone: true,
  imports: [CommonModule, SmartChatComponent, HeaderComponent, ProfilePictureDisplayComponent, RatingDisplayComponent, RatingStatsComponent],
  templateUrl: './worker-detail.component.html',
  styleUrl: './worker-detail.component.css'
})
export class WorkerDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private workerService = inject(WorkerControllerService);
  public authService = inject(AuthService);
  private http = inject(HttpClient);

  worker: WorkerResponseDto | null = null;
  ratingStats: WorkerRatingStatsDto | null = null;
  isLoading = false;
  error: string | null = null;
  workerId: string | null = null;
  
  // Chat properties
  showChat = false;
  currentUserId: string = '';
  currentUserName: string = ''; 
  currentUserType: 'CLIENT' | 'WORKER' = 'CLIENT';
  
  constructor(private dealService: JobDealControllerService) {}

  ngOnInit(): void {
    this.workerId = this.route.snapshot.paramMap.get('id');
    this.currentUserId = this.authService.getCurrentUserId() || '';
    this.currentUserName = this.authService.getCurrentUser()?.username || '';
    this.currentUserType = this.authService.getBusinessRole();
    
    if (this.workerId) {
      this.loadWorkerDetail();
      this.loadRatingStats();
    } else {
      this.error = 'Worker ID not found';
    }
  }

   getWorkerOwnerId(worker: WorkerResponseDto): string {
    // Some backends use the same id as the user id; others expose userId separately.
    // This handles both cases safely.
    return (worker.id as string) || (worker as any).userId || '';
  }

  getWorkerUsername(worker: WorkerResponseDto): string {
    // Prefer fullName; fall back to username if your DTO has it; else empty
    return (worker.fullName?.trim())
        || (worker as any).username?.trim()
        || '';
  }

  loadWorkerDetail(): void {
    if (!this.workerId) return;

    this.isLoading = true;
    this.error = null;

    this.workerService.getWorkerById(this.workerId).subscribe({
      next: (worker) => {
        this.worker = worker;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading worker details:', error);
        this.error = 'Failed to load worker details. Please try again.';
        this.isLoading = false;
      }
    });
  }

  loadRatingStats(): void {
    if (!this.workerId) return;

    this.dealService.getWorkerRatingStats(this.workerId).subscribe({
      next: (stats) => {
        this.ratingStats = stats;
      },
      error: (error) => {
        console.error('Error loading rating stats:', error);
        // Don't show error for rating stats, just log it
      }
    });
  }

  editWorker(): void {
    if (this.workerId) {
      this.router.navigate(['/workers', this.workerId, 'edit']);
    }
  }

  goBack(): void {
    this.router.navigate(['/client/workers']);
  }

  // New method to open chat
  openChat(): void {
    console.log('Opening chat with worker:', {
      workerId: this.workerId,
      workerName: this.worker?.fullName,
      currentUserId: this.currentUserId,
      currentUserType: this.currentUserType
    });
    this.showChat = true;
  }

  // New method to close chat
  closeChat(): void {
    this.showChat = false;
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

  callWorker(): void {
    if (this.worker?.phoneNumber) {
      window.open(`tel:${this.worker.phoneNumber}`, '_self');
    }
  }

  getDirections(): void {
    if (this.worker?.location) {
      const encodedLocation = encodeURIComponent(this.worker.location);
      window.open(`https://www.google.com/maps/search/${encodedLocation}`, '_blank');
    }
  }
}
