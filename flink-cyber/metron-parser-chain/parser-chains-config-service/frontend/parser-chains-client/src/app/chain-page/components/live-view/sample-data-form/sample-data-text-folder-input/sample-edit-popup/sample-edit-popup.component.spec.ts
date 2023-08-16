import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SampleEditPopupComponent } from './sample-edit-popup.component';

describe('SampleEditPopupComponent', () => {
  let component: SampleEditPopupComponent;
  let fixture: ComponentFixture<SampleEditPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SampleEditPopupComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleEditPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
