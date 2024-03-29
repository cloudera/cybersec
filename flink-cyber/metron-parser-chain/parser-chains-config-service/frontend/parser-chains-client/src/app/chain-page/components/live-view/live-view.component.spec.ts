/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

import {ComponentFixture, fakeAsync, flush, TestBed, tick, waitForAsync} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';
import {Store} from '@ngrx/store';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {NzSwitchModule} from 'ng-zorro-antd/switch';
import {NzTabsModule} from 'ng-zorro-antd/tabs';
import {NzSpinModule} from 'ng-zorro-antd/spin';
import {Subject} from 'rxjs';

import {executionTriggered, liveViewInitialized, onOffToggleChanged, sampleDataInputChanged} from './live-view.actions';
import {LiveViewComponent} from './live-view.component';
import {LiveViewState} from './live-view.reducers';
import {SampleDataType} from './models/sample-data.model';
import {SampleDataFormComponent} from "./sample-data-form/sample-data-form.component";
import {MockComponent} from "ng-mocks";
import {LiveViewResultComponent} from "./live-view-result/live-view-result.component";
import {LiveViewConsts} from "./live-view.consts";
import {NzFormModule} from "ng-zorro-antd/form";
import {CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA} from "@angular/core";


describe('LiveViewComponent', () => {
  let component: LiveViewComponent;
  let fixture: ComponentFixture<LiveViewComponent>;

  let mockStore: MockStore<{ 'live-view': LiveViewState }>;

  const initialState = {
    'live-view': {
      sampleData: {
        type: SampleDataType.MANUAL,
        source: '',
      },
      isLiveViewOn: true,
      isExecuting: false,
      result: undefined,
    }
  };

  const testSampleData = {
    type: SampleDataType.MANUAL,
    source: 'test sample data input',
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzTabsModule,
        NzSpinModule,
        NzSwitchModule,
        NzFormModule,
        FormsModule,
        RouterTestingModule
      ],
      providers: [
        provideMockStore({initialState}),
      ],
      declarations: [
        LiveViewComponent,
        MockComponent(SampleDataFormComponent),
        MockComponent(LiveViewResultComponent),
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA]
    })
      .compileComponents();

    mockStore = TestBed.inject(Store) as MockStore<{ 'live-view': LiveViewState }>;
    spyOn(mockStore, 'dispatch').and.callThrough();
  }));

  beforeEach(() => {
    spyOn(localStorage, 'getItem');

    fixture = TestBed.createComponent(LiveViewComponent);
    component = fixture.componentInstance;
    component.chainConfig$ = new Subject<unknown>();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch liveViewInitialized on init', () => {
    const action = liveViewInitialized();
    expect(mockStore.dispatch).toHaveBeenCalledWith(action);
  });

  it('should react on sampleData change', fakeAsync(() => {
    component.sampleDataChange$.next(testSampleData);

    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE);

    const action = sampleDataInputChanged({sampleData: testSampleData});
    expect(mockStore.dispatch).toHaveBeenCalledWith(action);
  }));

  it('should react on chain config change', fakeAsync(() => {
    mockStore.setState({
      'live-view': {
        ...initialState['live-view'],
        sampleData: testSampleData,
      }
    });
    fixture.detectChanges();

    (component.chainConfig$ as Subject<unknown>).next({});
    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE);

    const action = executionTriggered({sampleData: testSampleData, chainConfig: {}});

    expect(mockStore.dispatch).toHaveBeenCalledWith(action);
  }));

  it('should filter out events without sample data input', fakeAsync(() => {
    (component.chainConfig$ as Subject<unknown>).next({});
    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE);
    const action = executionTriggered({sampleData: testSampleData, chainConfig: {}});

    expect(mockStore.dispatch).not.toHaveBeenCalledWith(action);
  }));

  it('should hold back (debounce) executeTriggered', fakeAsync(() => {
    mockStore.setState({
      'live-view': {
        ...initialState['live-view'],
        sampleData: testSampleData,
      }
    });

    (component.chainConfig$ as Subject<unknown>).next({});

    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE / 2);
    const action = executionTriggered({sampleData: testSampleData, chainConfig: {}});

    expect(mockStore.dispatch).not.toHaveBeenCalledWith(action);

    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE / 2);

    expect(mockStore.dispatch).toHaveBeenCalledWith(action);
  }));

  it('should trigger onOffToggleChanged if toggle change', () => {
    component.featureToggleChange$.next(true);

    const action = onOffToggleChanged({value: true});
    expect(mockStore.dispatch).toHaveBeenCalledWith(action);
  });

  it('should persist selected tab on change', fakeAsync(() => {
    spyOn(localStorage, 'setItem');

    fixture.debugElement.query(By.css('.ant-tabs-tab:nth-child(2)')).nativeElement.click();
    fixture.detectChanges();

    tick(); // needed by the animation
    expect(localStorage.setItem).toHaveBeenCalledWith('liveViewSelectedTabIndex', '1');
    flush();
  }));

  it('should restore selected tab on init', () => {
    expect(localStorage.getItem).toHaveBeenCalledWith('liveViewSelectedTabIndex');
  });
});
