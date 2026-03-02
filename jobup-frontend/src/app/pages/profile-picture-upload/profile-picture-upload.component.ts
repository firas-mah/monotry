import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FileUploadService } from '../../generated-sources/openapi/api/fileUpload.service';
import { FileUploadResponseDto } from '../../generated-sources/openapi/model/fileUploadResponseDto';
import { AuthService } from '../../services/auth.service';
import { ProfilePictureDisplayComponent } from '../profile-picture-display/profile-picture-display.component';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-profile-picture-upload',
  standalone: true,
  imports: [CommonModule, ProfilePictureDisplayComponent],
  templateUrl: './profile-picture-upload.component.html',
  styleUrl: './profile-picture-upload.component.css'
})
export class ProfilePictureUploadComponent implements OnInit {
  @Input() userId: string = '';
  @Output() uploadComplete = new EventEmitter<FileUploadResponseDto>();
  @Input() username: string = '';
  @ViewChild(ProfilePictureDisplayComponent) profileDisplayComponent!: ProfilePictureDisplayComponent;

  currentProfilePicture: FileUploadResponseDto | null = null;
  isUploading = false;
  uploadError: string | null = null;

  constructor(
    private fileUploadService: FileUploadService,
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit() {
    if (!this.userId || !this.username) {
      this.authService.currentUser$.subscribe(user => {
        if (!this.userId && user?.userId) this.userId = user.userId;
        if (!this.username && user?.username) this.username = user.username;
        if (this.userId) this.loadCurrentProfilePicture();
      });
    } else {
      this.loadCurrentProfilePicture();
    }
  }

  private loadCurrentProfilePicture() {
    if (!this.userId) return;

    this.fileUploadService.getFilesByOwnerAndCategory(this.userId, 'PROFILE_PICTURE')
      .subscribe({
        next: async (resp: any) => {
          try {
            let files: FileUploadResponseDto[];
            if (resp instanceof Blob) {
              const text = await resp.text();
              files = JSON.parse(text) as FileUploadResponseDto[];
            } else {
              files = resp as FileUploadResponseDto[];
            }
            this.currentProfilePicture = files.length ? files[0] : null;
            console.log('Current profile picture loaded:', this.currentProfilePicture, files);
          } catch (e) {
            console.error('Failed to parse profile picture list:', e);
            this.currentProfilePicture = null;
          }
        },
        error: (error) => {
          console.error('Error loading current profile picture:', error);
        }
      });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.uploadProfilePicture(file);
    }
  }

  private uploadProfilePicture(file: File) {
    if (!this.userId) {
      this.uploadError = 'User ID is required';
      return;
    }

    this.isUploading = true;
    this.uploadError = null;

    // Create FormData for proper file upload
    const formData = new FormData();
    formData.append('file', file);

    // Use the custom upload method with proper multipart/form-data
    this.uploadFileWithFormData(formData, 'PROFILE_PICTURE', this.userId).subscribe({
      next: (response: FileUploadResponseDto) => {
        this.currentProfilePicture = response;
        this.isUploading = false;
        // Refresh the profile picture display
        if (this.profileDisplayComponent) {
          this.profileDisplayComponent.refreshProfilePicture();
        }
        this.uploadComplete.emit(response);
      },
      error: (error: any) => {
        this.uploadError = 'Failed to upload profile picture';
        this.isUploading = false;
        console.error('Upload error:', error);
      }
    });
  }

  removeProfilePicture() {
    if (this.currentProfilePicture?.id) {
      this.fileUploadService.deleteFile(this.currentProfilePicture.id).subscribe({
        next: () => {
          this.currentProfilePicture = null;
          // Refresh the profile picture display
          if (this.profileDisplayComponent) {
            this.profileDisplayComponent.refreshProfilePicture();
          }
        },
        error: (error) => {
          console.error('Error removing profile picture:', error);
        }
      });
    }
  }

  private uploadFileWithFormData(formData: FormData, category: string, ownerId: string): Observable<FileUploadResponseDto> {
    const url = `${environment.apiBaseUrl}/api/files/upload?category=${category}&ownerId=${ownerId}`;
    return this.http.post<FileUploadResponseDto>(url, formData);
  }
}
