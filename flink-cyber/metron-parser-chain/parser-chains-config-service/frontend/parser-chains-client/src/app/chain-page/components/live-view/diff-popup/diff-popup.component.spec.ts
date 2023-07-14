import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DiffPopupComponent } from './diff-popup.component';

describe('DiffPopupComponent', () => {
  let component: DiffPopupComponent;
  let fixture: ComponentFixture<DiffPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DiffPopupComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DiffPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
