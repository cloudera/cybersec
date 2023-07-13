import {ComponentFixture, TestBed} from '@angular/core/testing';

import {IndexingFormComponent} from './indexing-form.component';

describe('IndexingFormComponent', () => {
  let component: IndexingFormComponent;
  let fixture: ComponentFixture<IndexingFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ IndexingFormComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(IndexingFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
