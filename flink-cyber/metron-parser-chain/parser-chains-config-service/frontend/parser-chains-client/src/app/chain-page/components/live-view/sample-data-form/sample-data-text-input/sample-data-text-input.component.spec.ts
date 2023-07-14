import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SampleDataTextInputComponent } from './sample-data-text-input.component';

describe('SampleDataTextInputComponent', () => {
  let component: SampleDataTextInputComponent;
  let fixture: ComponentFixture<SampleDataTextInputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SampleDataTextInputComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleDataTextInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
