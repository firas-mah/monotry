import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { WorkerControllerService, WorkerUpdateDto, WorkerResponseDto } from '../../generated-sources/openapi';
import { AuthenticationService, UserUpdateRequestDto } from '../../generated-sources/openapi';
import { AuthService } from '../../services/auth.service';
import { ProfilePictureUploadComponent } from '../../pages/profile-picture-upload/profile-picture-upload.component';
import { FileUploadResponseDto } from '../../generated-sources/openapi/model/fileUploadResponseDto';

@Component({
  selector: 'app-profile-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ProfilePictureUploadComponent],
  templateUrl: './profile-edit.component.html',
  styleUrl: './profile-edit.component.css'
})
export class ProfileEditComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private workerService = inject(WorkerControllerService);
  private authService = inject(AuthService);
  private authController = inject(AuthenticationService);

  profileForm: FormGroup;
  isLoading = false;
  isSubmitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  loadError: string | null = null;
  currentUser: any = null;
  isWorker = false;
  currentUserId: string | undefined;

  // Job types for worker profile
  jobTypes = [
    'Plumber', 'Electrician', 'Carpenter', 'Painter',
    'Cleaner', 'Gardener', 'Mechanic', 'Cook',
    'Driver', 'Handyman', 'Other'
  ];

  constructor() {
    this.profileForm = this.fb.group({
      // Common fields for all users
      fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      
      // Worker-specific fields
      jobType: [''],
      phoneNumber: ['', [Validators.pattern(/^\+?[\d\s\-\(\)]+$/)]],
      location: ['', [Validators.minLength(2)]],
      description: ['', [Validators.minLength(10)]]
    });
  }

  ngOnInit(): void {
    this.currentUserId = this.authService.getCurrentUserId();
    this.isWorker = this.authService.isWorker();
    
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.populateCommonFields(user);
    });

    if (this.isWorker && this.currentUserId) {
      this.loadWorkerData();
    } else {
      this.isLoading = false;
    }
  }

  loadWorkerData(): void {
    if (!this.currentUserId) return;

    this.isLoading = true;
    this.loadError = null;

    this.workerService.getWorkerById(this.currentUserId).subscribe({
      next: (worker) => {
        this.populateWorkerFields(worker);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading worker data:', error);
        this.loadError = 'Failed to load worker data. Please try again.';
        this.isLoading = false;
      }
    });
  }

  populateCommonFields(user: any): void {
    this.profileForm.patchValue({
      fullName: user?.username || '', // Use username as fullName for clients
      email: user?.email || ''
    });
  }

  populateWorkerFields(worker: WorkerResponseDto): void {
    // Get the current user data to access email
    const currentUser = this.authService.getCurrentUser();
    
    this.profileForm.patchValue({
      fullName: worker.fullName || '',
      email: currentUser?.email || '', // Use email from user data
      jobType: worker.jobType || '',
      phoneNumber: worker.phoneNumber || '',
      location: worker.location || '',
      description: worker.description || ''
    });
  }

  async onSubmit(): Promise<void> {
    if (this.profileForm.valid && !this.isSubmitting) {
      this.isSubmitting = true;
      this.submitError = null;
      this.submitSuccess = false;

      const formValue = this.profileForm.value;
      
      // Update user account data (username and email) for all users
      const userUpdateData: UserUpdateRequestDto = {
        username: formValue.fullName, // Use fullName as username
        email: formValue.email
      };

      console.log('Sending update request with data:', userUpdateData);

      try {
        // First update user account data
        const response = await this.authController.updateCurrentUser(userUpdateData).toPromise();
        
        console.log('Raw response from backend:', response);
        console.log('Response type:', typeof response);
        
        let parsedResponse: any;
        
        // Handle Blob response (due to OpenAPI client configuration)
        if (response instanceof Blob) {
          try {
            const text = await response.text();
            parsedResponse = JSON.parse(text);
            console.log('Parsed Blob response:', parsedResponse);
          } catch (error) {
            console.error('Failed to parse Blob response:', error);
            this.submitError = 'Failed to parse server response. Please try again.';
            this.isSubmitting = false;
            return;
          }
        } else {
          parsedResponse = response;
        }
        
        // CRITICAL FIX: Update token properly
        if (parsedResponse && parsedResponse.token) {
          this.authService.updateToken(parsedResponse.token);
          console.log('Token updated successfully via AuthService');
        } else {
          console.warn('No token in response:', parsedResponse);
        }
        
        // CRITICAL FIX: Preserve existing roles and update user data
        const currentUser = this.authService.getCurrentUser();
        console.log('Current user before update:', currentUser);
        
        const updatedUserData = {
          roles: currentUser?.roles || [], // Always preserve existing roles
          userId: parsedResponse?.userId || currentUser?.userId,
          username: parsedResponse?.username || formValue.fullName, // Fallback to form value
          email: parsedResponse?.email || formValue.email // Fallback to form value
        };
        
        console.log('Preserving roles:', currentUser?.roles);
        console.log('Updated user data:', updatedUserData);
        
        // Update the AuthService with new user data
        this.authService.updateCurrentUserData(updatedUserData);
        
        // If user is a worker, also update worker-specific data
        if (this.isWorker && this.currentUserId) {
          const workerData: WorkerUpdateDto = {
            fullName: formValue.fullName,
            jobType: formValue.jobType,
            phoneNumber: formValue.phoneNumber,
            location: formValue.location,
            description: formValue.description
          };

          this.workerService.updateWorker(this.currentUserId, workerData).subscribe({
            next: (workerResponse) => {
              console.log('Worker profile updated successfully:', workerResponse);
              this.submitSuccess = true;
              this.isSubmitting = false;

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
          // For clients, just show success
          this.submitSuccess = true;
          this.isSubmitting = false;

          setTimeout(() => {
            this.router.navigate(['/client/home']);
          }, 2000);
        }
      } catch (error) {
        console.error('Error updating user account:', error);
        console.error('Error details:', error);
        this.submitError = 'Failed to update user account. Please try again.';
        this.isSubmitting = false;
      }
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.profileForm.controls).forEach(key => {
        this.profileForm.get(key)?.markAsTouched();
      });
    }
  }

  onPasswordChange(): void {
    this.router.navigate(['/change-password']);
  }

  goBack(): void {
    if (this.isWorker) {
      this.router.navigate(['/worker/worker-dashboard']);
    } else {
      this.router.navigate(['/client/home']);
    }
  }

  // Helper methods for template
  isFieldInvalid(fieldName: string): boolean {
    const field = this.profileForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.profileForm.get(fieldName);
    if (field && field.errors && (field.dirty || field.touched)) {
      if (field.errors['required']) {
        return `${this.getFieldDisplayName(fieldName)} is required`;
      }
      if (field.errors['minlength']) {
        return `${this.getFieldDisplayName(fieldName)} must be at least ${field.errors['minlength'].requiredLength} characters`;
      }
      if (field.errors['maxlength']) {
        return `${this.getFieldDisplayName(fieldName)} must be at most ${field.errors['maxlength'].requiredLength} characters`;
      }
      if (field.errors['email']) {
        return 'Please enter a valid email address';
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
      email: 'Email',
      jobType: 'Job Type',
      phoneNumber: 'Phone Number',
      location: 'Location',
      description: 'Description'
    };
    return displayNames[fieldName] || fieldName;
  }

  onProfilePictureUploaded(fileResponse: FileUploadResponseDto): void {
    console.log('Profile picture uploaded:', fileResponse);
    // The profile picture is automatically associated with the user
    // No additional action needed here
  }

  onProfilePictureRemoved(): void {
    console.log('Profile picture removed');
    // The profile picture has been removed
    // No additional action needed here
  }
}
