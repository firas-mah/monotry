import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RatingDisplayComponent } from '../rating-display/rating-display.component';
import { WorkerRatingStatsDto } from '../../generated-sources/openapi/model/workerRatingStatsDto';

@Component({
  selector: 'app-rating-stats',
  standalone: true,
  imports: [CommonModule, RatingDisplayComponent],
  templateUrl: './rating-stats.component.html',
  styleUrl: './rating-stats.component.css'
})
export class RatingStatsComponent implements OnInit {
  @Input() workerId: string = '';
  @Input() stats: WorkerRatingStatsDto | null = null;
  @Input() showDistribution: boolean = true;

  ngOnInit() {
    // Component can be used with pre-loaded stats or will load them if workerId is provided
  }

  get distributionData(): Array<{stars: number, count: number, percentage: number}> {
    if (!this.stats?.ratingDistribution || !this.stats.totalRatings) {
      return [];
    }

    const dist = this.stats.ratingDistribution;
    const total = this.stats.totalRatings;

    return [
      { stars: 5, count: dist.fiveStars || 0, percentage: ((dist.fiveStars || 0) / total) * 100 },
      { stars: 4, count: dist.fourStars || 0, percentage: ((dist.fourStars || 0) / total) * 100 },
      { stars: 3, count: dist.threeStars || 0, percentage: ((dist.threeStars || 0) / total) * 100 },
      { stars: 2, count: dist.twoStars || 0, percentage: ((dist.twoStars || 0) / total) * 100 },
      { stars: 1, count: dist.oneStar || 0, percentage: ((dist.oneStar || 0) / total) * 100 }
    ];
  }

  getStarArray(count: number): string[] {
    return Array(count).fill('★');
  }
}
