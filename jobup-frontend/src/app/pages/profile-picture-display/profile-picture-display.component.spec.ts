import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfilePictureDisplayComponent } from './profile-picture-display.component';

describe('ProfilePictureDisplayComponent', () => {
  let component: ProfilePictureDisplayComponent;
  let fixture: ComponentFixture<ProfilePictureDisplayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfilePictureDisplayComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ProfilePictureDisplayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
