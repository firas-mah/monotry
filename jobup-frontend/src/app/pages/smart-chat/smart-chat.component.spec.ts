import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SmartChatComponent } from './smart-chat.component';

describe('SmartChatComponent', () => {
  let component: SmartChatComponent;
  let fixture: ComponentFixture<SmartChatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SmartChatComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SmartChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
