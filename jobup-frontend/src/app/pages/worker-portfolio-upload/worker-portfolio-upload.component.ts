import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadService, FileCategory } from '../../services/file-upload.service';

export interface FileUploadResponse {
  id: string;
  originalFileName: string;
  downloadUrl: string;
  contentType: string;
  fileSize: number;
  category: string;
  uploadedAt: string;
}

@Component({
  selector: 'app-worker-portfolio-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './worker-portfolio-upload.component.html',
  styleUrl: './worker-portfolio-upload.component.css'
})
export class WorkerPortfolioUploadComponent implements OnInit {
  @Input() workerId: string = '';
  @Input() category: FileCategory = FileCategory.WORKER_PORTFOLIO;
  @Input() maxFiles: number = 5;
  @Output() filesUploaded = new EventEmitter<FileUploadResponse[]>();
  @Output() fileDeleted = new EventEmitter<string>();

  uploadedFiles: FileUploadResponse[] = [];
  isUploading = false;
  uploadProgress = 0;
  uploadError: string | null = null;
  isDragOver = false;

  constructor(private fileUploadService: FileUploadService) {}

  ngOnInit() {
    if (this.workerId) {
      this.loadExistingFiles();
    }
  }

  private loadExistingFiles() {
    // This would need to be implemented in the service
    // For now, we'll leave it empty and implement when the backend is ready
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = Array.from(event.dataTransfer?.files || []);
    if (files.length > 0) {
      this.uploadFiles(files);
    }
  }

  onFileSelected(event: any) {
    const files = Array.from(event.target.files) as File[];
    if (files.length > 0) {
      this.uploadFiles(files);
    }
  }

  private uploadFiles(files: File[]) {
    if (!this.workerId) {
      this.uploadError = 'Worker ID is required';
      return;
    }

    const filesToUpload = files.slice(0, this.maxFiles - this.uploadedFiles.length);

    if (filesToUpload.length === 0) {
      this.uploadError = 'Maximum number of files reached';
      return;
    }

    this.isUploading = true;
    this.uploadError = null;
    this.uploadProgress = 0;

    let completedUploads = 0;
    const totalUploads = filesToUpload.length;
    const uploadedResponses: FileUploadResponse[] = [];

    filesToUpload.forEach(file => {
      this.fileUploadService.uploadFile(file, this.category, this.workerId).subscribe({
        next: (progress) => {
          if (progress.type === 'progress') {
            this.uploadProgress = Math.round((progress.loaded / progress.total) * 100);
          } else if (progress.type === 'response') {
            const response = progress.body as FileUploadResponse;
            uploadedResponses.push(response);
            this.uploadedFiles.push(response);
            completedUploads++;

            if (completedUploads === totalUploads) {
              this.isUploading = false;
              this.uploadProgress = 0;
              this.filesUploaded.emit(uploadedResponses);
            }
          }
        },
        error: (error) => {
          console.error('Upload error:', error);
          completedUploads++;

          if (completedUploads === totalUploads) {
            this.isUploading = false;
            this.uploadProgress = 0;
            this.uploadError = 'Some files failed to upload';
          }
        }
      });
    });
  }

  removeFile(fileId: string) {
    // This would call the delete endpoint
    this.uploadedFiles = this.uploadedFiles.filter(f => f.id !== fileId);
    this.fileDeleted.emit(fileId);
  }

  canUploadMore(): boolean {
    return this.uploadedFiles.length < this.maxFiles;
  }

  getFileIcon(contentType: string): string {
    if (contentType.startsWith('image/')) return '🖼️';
    if (contentType === 'application/pdf') return '📄';
    if (contentType.includes('word')) return '📝';
    return '📎';
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}
