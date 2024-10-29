import {ComponentFixture, TestBed} from '@angular/core/testing';

import {OcsfFormComponent} from './ocsf-form.component';
import {ReactiveFormsModule} from "@angular/forms";
import {HttpClientTestingModule} from "@angular/common/http/testing";
import {NzMessageService} from "ng-zorro-antd/message";
import {ChainPageService} from "../../../services/chain-page.service";
import {provideMockStore} from "@ngrx/store/testing";
import {SampleDataType} from "../live-view/models/sample-data.model";

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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [OcsfFormComponent],
      imports: [
        HttpClientTestingModule,
        ReactiveFormsModule,
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
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
