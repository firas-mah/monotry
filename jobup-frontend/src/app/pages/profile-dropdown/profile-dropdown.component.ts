import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProfilePictureDisplayComponent } from '../profile-picture-display/profile-picture-display.component';

@Component({
  selector: 'app-profile-dropdown',
  standalone: true,
  imports: [CommonModule, ProfilePictureDisplayComponent],
  templateUrl: './profile-dropdown.component.html',
  styleUrl: './profile-dropdown.component.css'
})
export class ProfileDropdownComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  currentUser$ = this.authService.currentUser$;
  isDropdownOpen = false;
  currentUser: any = null;

  ngOnInit(): void {
    this.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  closeDropdown(): void {
    this.isDropdownOpen = false;
  }

  onEditProfile(): void {
    this.closeDropdown();
    // Navigate to unified profile edit page
    this.router.navigate(['/profile-edit']);
  }

  onLogout(): void {
    this.closeDropdown();
    this.authService.logout();
  }

  getInitials(username: string): string {
    if (!username) return '?';
    return username.charAt(0).toUpperCase();
  }

  getDisplayName(): string {
    if (!this.currentUser) return 'User';
    return this.currentUser.username || 'User';
  }

  isWorker(): boolean {
    return this.authService.isWorker();
  }
}