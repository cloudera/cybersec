import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NzButtonModule, NzFormModule, NzInputModule, NzMessageService } from 'ng-zorro-antd';

import { SampleDataType } from '../models/sample-data.model';

import { SampleDataFormComponent } from './sample-data-form.component';

export class MockService {}

describe('SampleDataFormComponent', () => {
  let component: SampleDataFormComponent;
  let fixture: ComponentFixture<SampleDataFormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        NzFormModule,
        NzButtonModule,
        NzInputModule,
      ],
      declarations: [ SampleDataFormComponent ],
      providers: [{ provide: NzMessageService, useClass: MockService}]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleDataFormComponent);
    component = fixture.componentInstance;

    component.sampleData = {
      type: SampleDataType.MANUAL,
      source: '',
    };

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch change action', () => {
    const sampleDataInput = fixture.debugElement.query(By.css('[data-qe-id="sample-input"]')).nativeElement;
    const expected = {
      type: SampleDataType.MANUAL,
      source: 'test sample data',
    };

    component.sampleDataChange.subscribe(sampleData => {
      expect(sampleData).toEqual(expected);
    });

    sampleDataInput.value = 'test sample data';
    sampleDataInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  });
});
