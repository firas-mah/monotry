import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkerPortfolioUploadComponent } from './worker-portfolio-upload.component';

describe('WorkerPortfolioUploadComponent', () => {
  let component: WorkerPortfolioUploadComponent;
  let fixture: ComponentFixture<WorkerPortfolioUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkerPortfolioUploadComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(WorkerPortfolioUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
