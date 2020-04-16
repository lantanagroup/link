import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectLocationsComponent } from './select-locations.component';

describe('SelectLocationsComponent', () => {
  let component: SelectLocationsComponent;
  let fixture: ComponentFixture<SelectLocationsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SelectLocationsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SelectLocationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
