import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthenticationService, UserUpdateRequestDto } from '../../generated-sources/openapi';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.css'
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);
  private authController = inject(AuthenticationService);

  passwordForm: FormGroup;
  isSubmitting = false;
  submitError: string | null = null;
  submitSuccess = false;

  constructor() {
    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup): { [key: string]: any } | null {
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;

    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  async onSubmit(): Promise<void> {
    if (this.passwordForm.valid && !this.isSubmitting) {
      this.isSubmitting = true;
      this.submitError = null;
      this.submitSuccess = false;

      const formValue = this.passwordForm.value;
      
      console.log('Password change submitted:', formValue);
      
      // Create the password update request
      const passwordUpdateData: UserUpdateRequestDto = {
        currentPassword: formValue.currentPassword,
        newPassword: formValue.newPassword
      };

      try {
        const response = await this.authController.updateCurrentUser(passwordUpdateData).toPromise();
        
        console.log('Password change response:', response);
        
        // Handle Blob response if needed
        let parsedResponse: any;
        if (response instanceof Blob) {
          try {
            const text = await response.text();
            parsedResponse = JSON.parse(text);
            console.log('Parsed password change response:', parsedResponse);
          } catch (error) {
            console.error('Failed to parse password change response:', error);
            this.submitError = 'Failed to parse server response. Please try again.';
            this.isSubmitting = false;
            return;
          }
        } else {
          parsedResponse = response;
        }
        
        // Update token if provided
        if (parsedResponse && parsedResponse.token) {
          this.authService.updateToken(parsedResponse.token);
          console.log('Token updated after password change');
        }
        
        // Update user data if needed
        if (parsedResponse) {
          const currentUser = this.authService.getCurrentUser();
          const updatedUserData = {
            roles: currentUser?.roles || [],
            userId: parsedResponse.userId || currentUser?.userId,
            username: parsedResponse.username || currentUser?.username,
            email: parsedResponse.email || currentUser?.email
          };
          this.authService.updateCurrentUserData(updatedUserData);
        }
        
        this.submitSuccess = true;
        this.isSubmitting = false;

        setTimeout(() => {
          this.goBack();
        }, 2000);
        
      } catch (error) {
        console.error('Error changing password:', error);
        this.submitError = 'Failed to change password. Please check your current password and try again.';
        this.isSubmitting = false;
      }
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.passwordForm.controls).forEach(key => {
        this.passwordForm.get(key)?.markAsTouched();
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/profile-edit']);
  }

  // Helper methods for template
  isFieldInvalid(fieldName: string): boolean {
    const field = this.passwordForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.passwordForm.get(fieldName);
    if (field && field.errors && (field.dirty || field.touched)) {
      if (field.errors['required']) {
        return `${this.getFieldDisplayName(fieldName)} is required`;
      }
      if (field.errors['minlength']) {
        return `${this.getFieldDisplayName(fieldName)} must be at least ${field.errors['minlength'].requiredLength} characters`;
      }
    }
    return '';
  }

  getPasswordMismatchError(): string {
    if (this.passwordForm.errors && this.passwordForm.errors['passwordMismatch']) {
      return 'Passwords do not match';
    }
    return '';
  }

  private getFieldDisplayName(fieldName: string): string {
    const displayNames: { [key: string]: string } = {
      currentPassword: 'Current Password',
      newPassword: 'New Password',
      confirmPassword: 'Confirm Password'
    };
    return displayNames[fieldName] || fieldName;
  }
}
