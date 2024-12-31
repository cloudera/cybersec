import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';

import {OcsfFormComponent} from './ocsf-form.component';
import {ReactiveFormsModule} from "@angular/forms";
import {HttpClientTestingModule} from "@angular/common/http/testing";
import {NzMessageService} from "ng-zorro-antd/message";
import {ChainPageService} from 'src/app/services/chain-page.service';
import {provideMockStore} from "@ngrx/store/testing";
import {SampleDataType} from "../live-view/models/sample-data.model";
import {MatCardModule} from '@angular/material/card';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';

describe('OcsfFormComponent', () => {
  let component: OcsfFormComponent;
  let fixture: ComponentFixture<OcsfFormComponent>;

  const liveViewInitialState = {
    sampleData: {
      type: SampleDataType.MANUAL,
      source: '',
    },
    isLiveViewOn: true,
    isExecuting: false,
    result: undefined,
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [OcsfFormComponent],
      imports: [
        HttpClientTestingModule,
        ReactiveFormsModule,
        MatCardModule,
        MatProgressSpinnerModule
      ],
      providers: [
        {provide: NzMessageService, useValue: jasmine.createSpyObj('NzMessageService', ['create'])},
        {provide: ChainPageService, useValue: jasmine.createSpyObj('ChainPageService', ['getIndexMappings'])},
        provideMockStore({
          initialState: {
            'live-view': liveViewInitialState
          },
          selectors: []
        })
      ],
    })
      .compileComponents();

    fixture = TestBed.createComponent(OcsfFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
