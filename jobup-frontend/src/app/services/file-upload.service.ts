import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface FileUploadResponse {
  id: string;
  originalFileName: string;
  downloadUrl: string;
  contentType: string;
  fileSize: number;
  fileType: string;
  category: string;
  uploadedAt: string;
  width?: number;
  height?: number;
  ownerId: string;
}

export interface UploadProgress {
  progress: number;
  status: 'uploading' | 'completed' | 'error';
  file?: File;
  response?: FileUploadResponse;
  error?: string;
}

export enum FileCategory {
  PROFILE_PICTURE = 'PROFILE_PICTURE',
  JOB_ATTACHMENT = 'JOB_ATTACHMENT',
  WORKER_PORTFOLIO = 'WORKER_PORTFOLIO',
  WORKER_CERTIFICATE = 'WORKER_CERTIFICATE'
}

@Injectable({
  providedIn: 'root'
})
export class FileUploadService {
  private baseUrl = `${environment.apiBaseUrl}/api/files`;
  private uploadProgress = new BehaviorSubject<UploadProgress | null>(null);

  constructor(private http: HttpClient) {}

  uploadFile(file: File, category: FileCategory, ownerId: string): Observable<UploadProgress> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('category', category);
    formData.append('ownerId', ownerId);

    const req = new HttpRequest('POST', `${this.baseUrl}/upload`, formData, {
      reportProgress: true
    });

    return this.http.request<FileUploadResponse>(req).pipe(
      map((event: HttpEvent<FileUploadResponse>) => {
        switch (event.type) {
          case HttpEventType.UploadProgress:
            const progress = event.total ? Math.round(100 * event.loaded / event.total) : 0;
            const uploadProgress: UploadProgress = {
              progress,
              status: 'uploading',
              file
            };
            this.uploadProgress.next(uploadProgress);
            return uploadProgress;

          case HttpEventType.Response:
            const completedProgress: UploadProgress = {
              progress: 100,
              status: 'completed',
              file,
              response: event.body!
            };
            this.uploadProgress.next(completedProgress);
            return completedProgress;

          default:
            return {
              progress: 0,
              status: 'uploading',
              file
            } as UploadProgress;
        }
      })
    );
  }

  getFilesByOwner(ownerId: string): Observable<FileUploadResponse[]> {
    return this.http.get<FileUploadResponse[]>(`${this.baseUrl}/owner/${ownerId}`);
  }

  getFilesByOwnerAndCategory(ownerId: string, category: FileCategory): Observable<FileUploadResponse[]> {
    return this.http.get<FileUploadResponse[]>(`${this.baseUrl}/owner/${ownerId}/category/${category}`);
  }

  getFileInfo(fileId: string): Observable<FileUploadResponse> {
    return this.http.get<FileUploadResponse>(`${this.baseUrl}/${fileId}`);
  }

  deleteFile(fileId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${fileId}`);
  }

  getDownloadUrl(fileId: string): string {
    return `${this.baseUrl}/${fileId}/download`;
  }

  getViewUrl(fileId: string): string {
    return `${this.baseUrl}/${fileId}/view`;
  }

  validateFile(file: File, category: FileCategory): { valid: boolean; error?: string } {
    // File size limits
    const maxImageSize = 5 * 1024 * 1024; // 5MB
    const maxDocumentSize = 10 * 1024 * 1024; // 10MB

    // Allowed file types
    const allowedImageTypes = ['image/jpeg', 'image/png', 'image/gif'];
    const allowedDocumentTypes = ['application/pdf', 'application/msword', 
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];

    // Check file size
    const isImage = allowedImageTypes.includes(file.type);
    const isDocument = allowedDocumentTypes.includes(file.type);
    
    if (!isImage && !isDocument) {
      return { valid: false, error: 'File type not supported' };
    }

    const maxSize = isImage ? maxImageSize : maxDocumentSize;
    if (file.size > maxSize) {
      const maxSizeMB = maxSize / (1024 * 1024);
      return { valid: false, error: `File size exceeds ${maxSizeMB}MB limit` };
    }

    // Category-specific validation
    if (category === FileCategory.PROFILE_PICTURE && !isImage) {
      return { valid: false, error: 'Profile pictures must be image files' };
    }

    return { valid: true };
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  getFileIcon(contentType: string): string {
    if (contentType.startsWith('image/')) {
      return 'image';
    } else if (contentType === 'application/pdf') {
      return 'picture_as_pdf';
    } else if (contentType.includes('word')) {
      return 'description';
    }
    return 'insert_drive_file';
  }

  isImageFile(contentType: string): boolean {
    return contentType.startsWith('image/');
  }

  getUploadProgress(): Observable<UploadProgress | null> {
    return this.uploadProgress.asObservable();
  }
}
