import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { WorkerControllerService, WorkerResponseDto } from '../../generated-sources/openapi';
import { HeaderComponent } from '../header/header.component';
import { RatingDisplayComponent } from '../rating-display/rating-display.component';
import { ProfilePictureDisplayComponent } from '../profile-picture-display/profile-picture-display.component';

@Component({
  selector: 'app-worker-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HeaderComponent, ProfilePictureDisplayComponent, RatingDisplayComponent],
  templateUrl: './worker-list.component.html',
  styleUrl: './worker-list.component.css'
})
export class WorkerListComponent implements OnInit {
  private fb = inject(FormBuilder);
  private workerService = inject(WorkerControllerService);
  private router = inject(Router);

  searchForm: FormGroup;
  workers: WorkerResponseDto[] = [];
  filteredWorkers: WorkerResponseDto[] = [];
  isLoading = false;
  error: string | null = null;

  jobTypes = ['All','Plumber','Electrician','Carpenter','Painter','Cleaner','Gardener','Mechanic','Cook','Driver','Handyman','Other'];

  constructor() {
    this.searchForm = this.fb.group({
      jobType: ['All'],
      location: ['']
    });

    this.searchForm.valueChanges.subscribe(() => this.filterWorkers());
  }

  ngOnInit(): void {
    this.loadAllWorkers();
  }

  getWorkerOwnerId(worker: WorkerResponseDto): string {
    return (worker.id as string) || (worker as any).userId || '';
  }

  getWorkerUsername(worker: WorkerResponseDto): string {
    return (worker.fullName?.trim()) || (worker as any).username?.trim() || '';
  }

  loadAllWorkers(): void {
    this.isLoading = true;
    this.error = null;

    // ✅ positional boolean param
    this.workerService.getAllWorkers(true).subscribe({
      next: (workers) => {
        this.workers = workers;
        this.filteredWorkers = workers;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading workers:', error);
        this.error = 'Failed to load workers. Please try again.';
        this.isLoading = false;
      }
    });
  }

  filterWorkers(): void {
    const { jobType, location } = this.searchForm.value;

    this.filteredWorkers = this.workers.filter(worker => {
      const matchesJobType = jobType === 'All' || worker.jobType === jobType;
      const matchesLocation = !location ||
        (worker.location && worker.location.toLowerCase().includes(location.toLowerCase()));
      return matchesJobType && matchesLocation;
    });
  }

  searchByJobType(): void {
    const jobType = this.searchForm.get('jobType')?.value;
    if (jobType && jobType !== 'All') {
      this.isLoading = true;
      // ✅ positional params: (jobType, excludeMe)
      this.workerService.searchByJobType(jobType, true).subscribe({
        next: (workers) => {
          this.workers = workers;
          this.filterWorkers();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error searching by job type:', error);
          this.error = 'Failed to search workers. Please try again.';
          this.isLoading = false;
        }
      });
    } else {
      this.loadAllWorkers();
    }
  }

  searchByLocation(): void {
    const location = this.searchForm.get('location')?.value;
    if (location) {
      this.isLoading = true;
      // ✅ positional params: (location, excludeMe)
      this.workerService.searchByLocation(location, true).subscribe({
        next: (workers) => {
          this.workers = workers;
          this.filterWorkers();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error searching by location:', error);
          this.error = 'Failed to search workers. Please try again.';
          this.isLoading = false;
        }
      });
    } else {
      this.loadAllWorkers();
    }
  }

  viewWorkerDetail(workerId: string | undefined): void {
    if (workerId) this.router.navigate(['/client/workers', workerId]);
  }

  clearSearch(): void {
    this.searchForm.reset({ jobType: 'All', location: '' });
    this.loadAllWorkers();
  }

  getStarRating(rating: number | undefined): string[] {
    const stars: string[] = [];
    const r = rating || 0;
    for (let i = 1; i <= 5; i++) stars.push(i <= r ? '★' : '☆');
    return stars;
  }
}
