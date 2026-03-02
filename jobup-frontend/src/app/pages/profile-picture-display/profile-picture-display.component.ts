import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadService } from '../../generated-sources/openapi/api/fileUpload.service';
import { FileUploadResponseDto } from '../../generated-sources/openapi/model/fileUploadResponseDto';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-profile-picture-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile-picture-display.component.html',
  styleUrl: './profile-picture-display.component.css'
})
export class ProfilePictureDisplayComponent implements OnInit {
  @Input() userId: string = '';
  @Input() size: 'small' | 'medium' | 'large' = 'medium';
  @Input() showFallback: boolean = true;

  @Input() preferredFileId: string | null = null;

  profilePicture: FileUploadResponseDto | null = null;
  isLoading = false;

  /** Bind this in the template instead of calling getImageUrl() */
  imageUrl = '';
  /** Used only when you want to force-refresh after a new upload */
  private cacheToken = '';
  @Input() username: string = '';

  constructor(private fileUploadService: FileUploadService) {}

  ngOnInit() {
    if (this.userId) {
      this.loadProfilePicture();
    }
  }

  getInitial(): string {
    const name = (this.username || '').trim();
    if (name) return name.charAt(0).toUpperCase();
    if (this.userId) return this.userId.charAt(0).toUpperCase();
    return '?';
  }

  private pickFile(files: FileUploadResponseDto[]): FileUploadResponseDto | null {
    // 1) Prefer the file referenced by the user/profile if available
    if (this.preferredFileId) {
      const m = files.find(f => f.id === this.preferredFileId);
      if (m) return m;
    }
    // 2) Otherwise pick the *newest* by updatedAt/ uploadedAt
    const t = (x?: string) => x ? new Date(x).getTime() : 0;
    files.sort((a,b) =>
      (t(b.updatedAt || b.uploadedAt) - t(a.updatedAt || a.uploadedAt))
    );
    return files[0] ?? null;
  }
  private loadProfilePicture() {
    if (!this.userId) return;
    this.isLoading = true;

    this.fileUploadService.getFilesByOwnerAndCategory(this.userId, 'PROFILE_PICTURE')
      .subscribe({
        next: async (resp: any) => {
          try {
            const files: FileUploadResponseDto[] =
              resp instanceof Blob ? JSON.parse(await resp.text()) : resp;

            this.profilePicture = this.pickFile(files);
            this.setImageUrl();
          } catch (e) {
            console.error('parse error', e);
            this.profilePicture = null; this.imageUrl = '';
          } finally {
            this.isLoading = false;
          }
        },
        error: err => {
          console.error('load error', err);
          this.profilePicture = null; this.imageUrl = '';
          this.isLoading = false;
        }
      });
  }

  /** Compute a stable URL once per load/refresh */
  private setImageUrl() {
    if (!this.profilePicture?.id) { this.imageUrl = ''; return; }
    const v = this.cacheToken ? `?v=${this.cacheToken}` : '';
    this.imageUrl = `${environment.apiBaseUrl}/api/files/${this.profilePicture.id}/view${v}`;
  }

  /** Call this when you want to bust cache after an upload */
  private bumpCache() {
    this.cacheToken = String(Date.now());
    this.setImageUrl();
  }

  // Called by the uploader after a successful upload
  refreshProfilePicture(): void {
    this.cacheToken = String(Date.now());   // cache-bust
    this.loadProfilePicture();
  }

  getSizeClass(): string {
    return `profile-picture-${this.size}`;
  }

  onImageError(event: any): void {
    console.error('Image failed to load:', {
      src: (event.target as HTMLImageElement).src,
      userId: this.userId,
      profilePicture: this.profilePicture
    });
    this.profilePicture = null;
    this.imageUrl = '';
  }

  onImageLoad(event: any): void {
    console.log('Image loaded successfully:', {
      src: (event.target as HTMLImageElement).src,
      userId: this.userId
    });
  }
}
