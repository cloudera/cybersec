import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SampleDataTextFolderInputComponent } from './sample-data-text-folder-input.component';

describe('SampleDataTextFolderInputComponent', () => {
  let component: SampleDataTextFolderInputComponent;
  let fixture: ComponentFixture<SampleDataTextFolderInputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SampleDataTextFolderInputComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleDataTextFolderInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
