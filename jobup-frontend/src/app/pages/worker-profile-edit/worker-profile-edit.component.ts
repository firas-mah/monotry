import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { WorkerControllerService, WorkerUpdateDto, WorkerResponseDto } from '../../generated-sources/openapi';
import { AuthService } from '../../services/auth.service';
import { WorkerPortfolioUploadComponent, FileUploadResponse } from '../../pages/worker-portfolio-upload/worker-portfolio-upload.component';
import { FileCategory } from '../../services/file-upload.service';

@Component({
  selector: 'app-worker-profile-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, WorkerPortfolioUploadComponent],
  templateUrl: './worker-profile-edit.component.html',
  styleUrl: './worker-profile-edit.component.css'
})
export class WorkerProfileEditComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private workerService = inject(WorkerControllerService);
  private authService = inject(AuthService);

  workerForm: FormGroup;
  isLoading = false;
  isSubmitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  loadError: string | null = null;
  currentUserId: string | undefined;
  portfolioFiles: FileUploadResponse[] = [];
  certificateFiles: FileUploadResponse[] = [];
  FileCategory = FileCategory;

  jobTypes = [
    'Plumber', 'Electrician', 'Carpenter', 'Painter',
    'Cleaner', 'Gardener', 'Mechanic', 'Cook',
    'Driver', 'Handyman', 'Other'
  ];

  constructor() {
    this.workerForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      jobType: ['', Validators.required],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[\d\s\-\(\)]+$/)]],
      location: ['', [Validators.required, Validators.minLength(2)]],
      description: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  ngOnInit(): void {
    this.currentUserId = this.authService.getCurrentUserId();
    if (this.currentUserId) {
      this.loadWorkerData();
    } else {
      this.loadError = 'User ID not found';
    }
  }

  loadWorkerData(): void {
    if (!this.currentUserId) return;

    this.isLoading = true;
    this.loadError = null;

    this.workerService.getWorkerById(this.currentUserId).subscribe({
      next: (worker) => {
        this.populateForm(worker);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading worker data:', error);
        this.loadError = 'Failed to load worker data. Please try again.';
        this.isLoading = false;
      }
    });
  }

  populateForm(worker: WorkerResponseDto): void {
    this.workerForm.patchValue({
      fullName: worker.fullName || '',
      jobType: worker.jobType || '',
      phoneNumber: worker.phoneNumber || '',
      location: worker.location || '',
      description: worker.description || ''
    });
  }

  onSubmit(): void {
    if (this.workerForm.valid && !this.isSubmitting && this.currentUserId) {
      this.isSubmitting = true;
      this.submitError = null;
      this.submitSuccess = false;

      const workerData: WorkerUpdateDto = {
        fullName: this.workerForm.value.fullName,
        jobType: this.workerForm.value.jobType,
        phoneNumber: this.workerForm.value.phoneNumber,
        location: this.workerForm.value.location,
        description: this.workerForm.value.description
      };

      this.workerService.updateWorker(this.currentUserId, workerData).subscribe({
        next: (response) => {
          console.log('Worker profile updated successfully:', response);
          this.submitSuccess = true;
          this.isSubmitting = false;

          // Redirect to worker dashboard after successful update
          setTimeout(() => {
            this.router.navigate(['/worker/worker-dashboard']);
          }, 2000);
        },
        error: (error) => {
          console.error('Error updating worker profile:', error);
          this.submitError = 'Failed to update worker profile. Please try again.';
          this.isSubmitting = false;
        }
      });
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.workerForm.controls).forEach(key => {
        this.workerForm.get(key)?.markAsTouched();
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/worker/worker-dashboard']);
  }

  // Helper methods for template
  isFieldInvalid(fieldName: string): boolean {
    const field = this.workerForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.workerForm.get(fieldName);
    if (field && field.errors && (field.dirty || field.touched)) {
      if (field.errors['required']) {
        return `${this.getFieldDisplayName(fieldName)} is required`;
      }
      if (field.errors['minlength']) {
        return `${this.getFieldDisplayName(fieldName)} must be at least ${field.errors['minlength'].requiredLength} characters`;
      }
      if (field.errors['pattern']) {
        return 'Please enter a valid phone number';
      }
    }
    return '';
  }

  private getFieldDisplayName(fieldName: string): string {
    const displayNames: { [key: string]: string } = {
      fullName: 'Full Name',
      jobType: 'Job Type',
      phoneNumber: 'Phone Number',
      location: 'Location',
      description: 'Description'
    };
    return displayNames[fieldName] || fieldName;
  }

  onPortfolioFilesUploaded(files: FileUploadResponse[]) {
    this.portfolioFiles = files;
    console.log('Portfolio files uploaded:', files);
  }

  onPortfolioFileDeleted(fileId: string) {
    this.portfolioFiles = this.portfolioFiles.filter(f => f.id !== fileId);
    console.log('Portfolio file deleted:', fileId);
  }

  onCertificateFilesUploaded(files: FileUploadResponse[]) {
    this.certificateFiles = files;
    console.log('Certificate files uploaded:', files);
  }

  onCertificateFileDeleted(fileId: string) {
    this.certificateFiles = this.certificateFiles.filter(f => f.id !== fileId);
    console.log('Certificate file deleted:', fileId);
  }
}