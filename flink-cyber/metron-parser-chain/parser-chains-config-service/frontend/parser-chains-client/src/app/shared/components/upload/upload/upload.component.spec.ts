import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import { UploadComponent } from './upload.component';
import {MatIconModule} from '@angular/material/icon';

describe('UploadComponent', () => {
  let component: UploadComponent;
  let fixture: ComponentFixture<UploadComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [MatIconModule],
      declarations: [ UploadComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
