import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {SampleDataTextFolderInputComponent} from './sample-data-text-folder-input.component';
import {provideMockStore} from "@ngrx/store/testing";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {NzFormModule} from "ng-zorro-antd/form";
import {NzTableModule} from "ng-zorro-antd/table";
import {NzInputModule} from "ng-zorro-antd/input";
import {NoopAnimationsModule} from "@angular/platform-browser/animations";
import {MockComponent} from "ng-mocks";
import {SampleEditPopupComponent} from "./sample-edit-popup/sample-edit-popup.component";
import {NzIconModule} from "ng-zorro-antd/icon";
import {
  CheckCircleTwoTone,
  ClockCircleTwoTone,
  CloseCircleTwoTone,
  FolderOutline
} from "@ant-design/icons-angular/icons";
import {TextDiffViewComponent} from "../../text-diff-view/text-diff-view.component";
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from "@angular/core";

describe('SampleDataTextFolderInputComponent', () => {
  let component: SampleDataTextFolderInputComponent;
  let fixture: ComponentFixture<SampleDataTextFolderInputComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ReactiveFormsModule,
        NzFormModule,
        NzTableModule,
        NzInputModule,
        NzIconModule.forRoot([FolderOutline, CheckCircleTwoTone, CloseCircleTwoTone, ClockCircleTwoTone]),
        NoopAnimationsModule
      ],
      declarations: [SampleDataTextFolderInputComponent, MockComponent(SampleEditPopupComponent), MockComponent(TextDiffViewComponent)],
      providers: [provideMockStore({
        initialState: {
          isTestExecuting: false,
          isFetchExecuting: false,
          isSaveExecuting: false,
          editModalShown: false,
          chainId: '123',
          sampleData: [{
            id: 1,
            name: 'foo',
            description: 'foo-desc',
            source: 'foo-source',
            expectedFailure: false,
            expectedResult: 'foo-res'
          }],
          sampleFolderPath: 'path/to/folder',
        }
      })],
      schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleDataTextFolderInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
