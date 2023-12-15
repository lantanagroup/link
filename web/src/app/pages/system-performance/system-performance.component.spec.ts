import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SystemPerformanceComponent } from './system-performance.component';

describe('SystemPerformanceComponent', () => {
  let component: SystemPerformanceComponent;
  let fixture: ComponentFixture<SystemPerformanceComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SystemPerformanceComponent]
    });
    fixture = TestBed.createComponent(SystemPerformanceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
