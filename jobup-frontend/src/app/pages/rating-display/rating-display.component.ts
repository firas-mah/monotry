import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-rating-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rating-display.component.html',
  styleUrl: './rating-display.component.css'
})
export class RatingDisplayComponent {
  @Input() rating: number = 0;
  @Input() totalRatings: number = 0;
  @Input() size: 'small' | 'medium' | 'large' = 'medium';
  @Input() showText: boolean = true;
  @Input() showCount: boolean = true;

  get stars(): string[] {
    const stars = [];
    const ratingValue = this.rating || 0;

    for (let i = 1; i <= 5; i++) {
      if (i <= ratingValue) {
        stars.push('★');
      } else if (i - 0.5 <= ratingValue) {
        stars.push('☆'); // Could be enhanced to show half stars
      } else {
        stars.push('☆');
      }
    }
    return stars;
  }

  get sizeClass(): string {
    return `rating-${this.size}`;
  }

  get formattedRating(): string {
    return (this.rating || 0).toFixed(1);
  }
}
