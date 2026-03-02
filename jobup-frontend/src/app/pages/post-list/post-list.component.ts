import { Component, OnInit } from '@angular/core';
import { JobPostControllerService, JobPostDto, FileUploadResponseDto } from '../../generated-sources/openapi';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { FileUploadService } from '../../services/file-upload.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-post-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './post-list.component.html',
  styleUrls: ['./post-list.component.css']
})
export class PostListComponent implements OnInit {
  posts: JobPostDto[] = [];
  currentUserId: string = '';
  currentUserName: string = '';
  commentInputs: { [postId: string]: string } = {};
  showComments: { [postId: string]: boolean } = {};
  postAttachments: { [postId: string]: FileUploadResponseDto[] } = {};

  constructor(
    private postService: JobPostControllerService,
    private auth: AuthService,
    private router: Router,
    private fileUploadService: FileUploadService
  ) {}

  ngOnInit() {
    this.currentUserId = this.auth.getCurrentUserId() || '';
    this.currentUserName = this.auth.getCurrentUser()?.username || '';
    this.loadPosts();
  }

  loadPosts() {
    this.postService.getAllPosts().subscribe({
      next: (posts) => {
        this.posts = posts;
        // Load attachments for posts that have them
        posts.forEach(post => {
          if (post.attachmentFileIds && post.attachmentFileIds.length > 0) {
            this.loadPostAttachments(post.id!);
          }
        });
      }
    });
  }

  private loadPostAttachments(postId: string) {
    this.postService.getPostAttachments(postId).subscribe({
      next: (files) => {
        this.postAttachments[postId] = files;
      },
      error: (error) => {
        console.error('Error loading attachments for post', postId, error);
      }
    });
  }

  like(post: JobPostDto) {
    // optimistic toggle
    const me = this.currentUserId;
    const liked = post.likes?.includes(me);
    post.likes = liked ? (post.likes || []).filter(id => id !== me) : [...(post.likes || []), me];

    this.postService.likePost(post.id!).subscribe({
      next: (updated) => Object.assign(post, updated),
      error: () => this.loadPosts() // rollback by reloading on error
    });
  }

  save(post: JobPostDto) {
    const me = this.currentUserId;
    const saved = post.savedBy?.includes(me);
    post.savedBy = saved ? (post.savedBy || []).filter(id => id !== me) : [...(post.savedBy || []), me];

    this.postService.savePost(post.id!).subscribe({
      next: (updated) => Object.assign(post, updated),
      error: () => this.loadPosts()
    });
  }

  toggleComments(post: JobPostDto) {
    this.showComments[post.id!] = !this.showComments[post.id!];
  }

  addComment(post: JobPostDto) {
    const content = this.commentInputs[post.id!]?.trim();
    if (!content) return;
    this.postService.addComment(post.id!, { content }).subscribe({
      // ❌ Only content is sent, authorName is missing
      next: (updated) => {
        Object.assign(post, updated);
        this.commentInputs[post.id!] = '';
        // Instant refresh to show the new comment
        this.loadPosts();
      }
    });
  }

  // Track by functions for better performance
  trackByPostId(index: number, post: JobPostDto): string {
    return post.id || index.toString();
  }

  trackByCommentId(index: number, comment: any): string {
    return comment.id || index.toString();
  }

  // Helper methods for UI
  isLikedByCurrentUser(post: JobPostDto): boolean {
    return post.likes?.includes(this.currentUserId) || false;
  }

  isSavedByCurrentUser(post: JobPostDto): boolean {
    return post.savedBy?.includes(this.currentUserId) || false;
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name.split(' ')
      .map(word => word.charAt(0).toUpperCase())
      .slice(0, 2)
      .join('');
  }

  onKeyDown(event: KeyboardEvent, post: JobPostDto): void {
    if (event.ctrlKey && event.key === 'Enter') {
      this.addComment(post);
    }
  }

  goToSavedPosts(): void {
    this.router.navigate(['/worker/saved-posts']);
  }

  getFileUrl(file: FileUploadResponseDto): string {
    if (file.downloadUrl?.startsWith('http')) {
      return file.downloadUrl;
    }
    return `${environment.apiBaseUrl}/api/files/${file.id}/view`;
  }

  isImage(file: FileUploadResponseDto): boolean {
    return file.contentType?.startsWith('image/') || false;
  }

  isPdf(file: FileUploadResponseDto): boolean {
    return file.contentType === 'application/pdf';
  }

  getFileIcon(file: FileUploadResponseDto): string {
    if (this.isImage(file)) return '🖼️';
    if (this.isPdf(file)) return '📄';
    if (file.contentType?.includes('word')) return '📝';
    return '📎';
  }

  getImages(postId: string): FileUploadResponseDto[] {
    return this.postAttachments[postId]?.filter(file => this.isImage(file)) || [];
  }

  getNonImages(postId: string): FileUploadResponseDto[] {
    return this.postAttachments[postId]?.filter(file => !this.isImage(file)) || [];
  }

  formatFileSize(bytes: number | undefined): string {
    if (!bytes) return '';
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
  }

  openImageModal(file: FileUploadResponseDto) {
    // Open image in modal or new tab
    window.open(this.getFileUrl(file), '_blank');
  }

  downloadFile(file: FileUploadResponseDto) {
    const url = `${environment.apiBaseUrl}/api/files/${file.id}/download`;
    window.open(url, '_blank');
  }
}
