import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkerProfileEditComponent } from './worker-profile-edit.component';

describe('WorkerProfileEditComponent', () => {
  let component: WorkerProfileEditComponent;
  let fixture: ComponentFixture<WorkerProfileEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkerProfileEditComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(WorkerProfileEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
