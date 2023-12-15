import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResourceContentsComponent } from './resource-contents.component';

describe('ResourceContentsComponent', () => {
  let component: ResourceContentsComponent;
  let fixture: ComponentFixture<ResourceContentsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ResourceContentsComponent]
    });
    fixture = TestBed.createComponent(ResourceContentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
