import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TenantReportsComponent } from './tenant-reports.component';

describe('TenantReportsComponent', () => {
  let component: TenantReportsComponent;
  let fixture: ComponentFixture<TenantReportsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TenantReportsComponent]
    });
    fixture = TestBed.createComponent(TenantReportsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
