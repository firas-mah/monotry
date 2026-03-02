import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RatingStatsComponent } from './rating-stats.component';

describe('RatingStatsComponent', () => {
  let component: RatingStatsComponent;
  let fixture: ComponentFixture<RatingStatsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatingStatsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RatingStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
