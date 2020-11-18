import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MeasureEvalComponent } from './measure-eval.component';

describe('MeasureEvalComponent', () => {
  let component: MeasureEvalComponent;
  let fixture: ComponentFixture<MeasureEvalComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MeasureEvalComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MeasureEvalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
