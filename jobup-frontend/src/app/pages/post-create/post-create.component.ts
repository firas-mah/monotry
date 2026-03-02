import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { JobPostDto } from '../../generated-sources/openapi';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-post-create',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './post-create.component.html',
  styleUrl: './post-create.component.css'
})
export class PostCreateComponent {
  title = '';
  description = '';
  location = '';
  isLoading = false;
  success = false;
  error: string | null = null;

  // NEW: files selected in the form
  selectedFiles: File[] = [];

  constructor(private http: HttpClient, private auth: AuthService) {}

  onFilesSelected(evt: Event) {
    const input = evt.target as HTMLInputElement;
    if (!input.files?.length) return;
    // optionally cap to 3 files
    const toAdd = Array.from(input.files).slice(0, 3 - this.selectedFiles.length);
    this.selectedFiles.push(...toAdd);
    // reset input so the same file can be re-picked if removed
    input.value = '';
  }

  removeFile(i: number) {
    this.selectedFiles.splice(i, 1);
  }

  createPost() {
    this.isLoading = true;
    this.error = null;

    // minimal payload; backend sets createdBy from Principal
    const post: Partial<JobPostDto> = {
      title: this.title,
      description: this.description,
      location: this.location
    };

    const fd = new FormData();
    fd.append('post', new Blob([JSON.stringify(post)], { type: 'application/json' }));
    for (const f of this.selectedFiles) {
      fd.append('files', f);
    }

    const apiBase = 'http://localhost:8083/JobUp/api';
    const headers = new HttpHeaders({
      // if you already have an auth interceptor, you can remove this header
      Authorization: `Bearer ${this.auth.getToken()}`
    });

    this.http.post<JobPostDto>(`${apiBase}/posts/multipart`, fd, { headers }).subscribe({
      next: () => {
        this.success = true;
        this.isLoading = false;
        this.title = '';
        this.description = '';
        this.location = '';
        this.selectedFiles = [];
      },
      error: (e) => {
        console.error(e);
        this.error = e?.error?.message || 'Failed to create post';
        this.isLoading = false;
      }
    });
  }
}
