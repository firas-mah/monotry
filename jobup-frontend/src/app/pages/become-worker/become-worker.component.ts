import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { WorkerControllerService, WorkerCreateDto } from '../../generated-sources/openapi';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-become-worker',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './become-worker.component.html',
  styleUrl: './become-worker.component.css'
})
export class BecomeWorkerComponent implements OnInit{
  private fb = inject(FormBuilder);
  private workerService = inject(WorkerControllerService);
  private authService = inject(AuthService);
  private router = inject(Router);

  isLoading = false;
  error: string | null = null;
  currentUser$ = this.authService.currentUser$;

  workerForm = this.fb.group({
    jobType: ['', Validators.required],
    phoneNumber: ['', Validators.required],
    location: ['', Validators.required],
    description: ['']
  });

  jobTypes = [
    'Plumber', 'Electrician', 'Carpenter', 'Painter',
    'Cleaner', 'Gardener', 'Mechanic', 'Cook',
    'Driver', 'Handyman', 'Other'
  ];

  ngOnInit(): void {
    // Check if user is already a worker
    if (this.authService.isWorker()) {
      console.log('User is already a worker, redirecting to worker dashboard');
      this.router.navigate(['/worker/worker-dashboard']);
      return;
    }
  }

  onSubmit(): void {
    if (this.workerForm.valid) {
      this.isLoading = true;
      this.error = null;

      const formValue = this.workerForm.value;
      const userId = this.authService.getCurrentUserId();
      console.log('userId being sent:', userId);
      
      const workerData: WorkerCreateDto = {
        userId: userId ?? undefined,
        jobType: formValue.jobType || undefined,
        phoneNumber: formValue.phoneNumber || undefined,
        location: formValue.location || undefined,
        description: formValue.description || undefined
      };

      console.log('Creating worker with data:', workerData);

      this.workerService.createWorker(workerData).subscribe({
        next: (response) => {
          console.log('Worker created successfully:', response);
          this.isLoading = false;
          
          // Simple navigation without complex refresh logic
          this.router.navigate(['/worker/worker-dashboard']);
        },
        error: (error) => {
          console.error('Error creating worker profile:', error);
          this.isLoading = false;
          this.error = 'Failed to create worker profile. Please try again.';
        }
      });
    } else {
      console.log('Form is invalid:', this.workerForm.errors);
      this.error = 'Please fill in all required fields.';
    }
  }

  onCancel(): void {
    this.router.navigate(['/client/home']);
  }

  logout(): void {
    this.authService.logout();
  }
}
