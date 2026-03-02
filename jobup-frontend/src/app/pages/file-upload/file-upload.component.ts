import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OnChanges, SimpleChanges } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { FileUploadService } from '../../generated-sources/openapi/api/fileUpload.service';
import { FileUploadResponseDto } from '../../generated-sources/openapi/model/fileUploadResponseDto';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-upload.component.html',
  styleUrl: './file-upload.component.css'
})
export class FileUploadComponent implements OnInit, OnChanges {
  @Input() category: 'PROFILE_PICTURE' | 'JOB_ATTACHMENT' | 'WORKER_PORTFOLIO' | 'WORKER_CERTIFICATE' = 'JOB_ATTACHMENT';
  @Input() ownerId: string = '';
  @Input() allowMultiple: boolean = false;
  @Input() maxFiles: number = 1;
  @Output() filesUploaded = new EventEmitter<FileUploadResponseDto[]>();
  @Output() fileDeleted = new EventEmitter<string>();

  uploadedFiles: FileUploadResponseDto[] = [];
  isUploading = false;
  uploadError: string | null = null;

  constructor(private fileUploadService: FileUploadService) {}

  ngOnInit() {
    if (this.ownerId) {
      this.loadExistingFiles();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
  if (changes['ownerId'] && this.ownerId) {
    this.loadExistingFiles(); // now that we have a real owner, fetch any existing
  }
}

  private loadExistingFiles() {
    this.fileUploadService.getFilesByOwnerAndCategory(this.ownerId, this.category).subscribe({
      next: (files) => {
        this.uploadedFiles = files;
      },
      error: (error) => {
        console.error('Error loading existing files:', error);
      }
    });
  }

  onFileSelected(event: any) {
    const files = Array.from(event.target.files) as File[];
    if (files.length > 0) {
      this.uploadFiles(files);
    }
  }

  private uploadFiles(files: File[]) {
    if (!this.ownerId) {
      this.uploadError = 'Owner ID is required';
      return;
    }

    const filesToUpload = this.allowMultiple ?
      files.slice(0, this.maxFiles - this.uploadedFiles.length) :
      [files[0]];

    this.isUploading = true;
    this.uploadError = null;

    const uploadRequests = filesToUpload.map(file => {
        this.fileUploadService.uploadFile(this.category, this.ownerId, file)
    });

    forkJoin(uploadRequests.length ? uploadRequests : [of(null)]).subscribe({
      next: (responses) => {
        const valid = (responses || []).filter(r => !!r) as FileUploadResponseDto[];
        this.uploadedFiles.push(...valid);
        this.isUploading = false;
        this.filesUploaded.emit(valid);
      },
      error: (err) => {
        this.uploadError = 'Failed to upload files';
        this.isUploading = false;
        console.error('Upload error:', err);
      }
    });

  }

  removeFile(fileId: string) {
    this.fileUploadService.deleteFile(fileId).subscribe({
      next: () => {
        this.uploadedFiles = this.uploadedFiles.filter(f => f.id !== fileId);
        this.fileDeleted.emit(fileId);
      },
      error: (error) => {
        console.error('Error removing file:', error);
      }
    });
  }

  canUploadMore(): boolean {
    return this.allowMultiple ? this.uploadedFiles.length < this.maxFiles : this.uploadedFiles.length === 0;
  }
}
