import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {SampleEditPopupComponent} from './sample-edit-popup.component';
import {provideMockStore} from "@ngrx/store/testing";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {NzFormModule} from "ng-zorro-antd/form";
import {NzModalModule} from "ng-zorro-antd/modal";

describe('SampleEditPopupComponent', () => {
  let component: SampleEditPopupComponent;
  let fixture: ComponentFixture<SampleEditPopupComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        FormsModule,
        NzFormModule,
        NoopAnimationsModule,
        ReactiveFormsModule
      ],
      declarations: [SampleEditPopupComponent],
      providers: [
        provideMockStore({
          initialState: {
            isTestExecuting: false,
            isFetchExecuting: false,
            isSaveExecuting: false,
            editModalShown: false,
            chainId: '123',
            sampleData: [],
            sampleFolderPath: 'path/to/folder',
          }
        })
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleEditPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
