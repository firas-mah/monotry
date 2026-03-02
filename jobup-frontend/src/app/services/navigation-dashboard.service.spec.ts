import { TestBed } from '@angular/core/testing';

import { NavigationDashboardService } from './navigation-dashboard.service';

describe('NavigationDashboardService', () => {
  let service: NavigationDashboardService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NavigationDashboardService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
