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

import { Component, Input } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { Store } from '@ngrx/store';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { Subject } from 'rxjs';

import {
  executionTriggered,
  liveViewInitialized,
  onOffToggleChanged,
  sampleDataInputChanged
} from './live-view.actions';
import { LiveViewComponent } from './live-view.component';
import { LiveViewConsts } from './live-view.consts';
import { LiveViewState } from './live-view.reducers';
import { EntryParsingResultModel } from './models/live-view.model';
import { SampleDataModel, SampleDataType } from './models/sample-data.model';

@Component({
  selector: 'app-sample-data-form',
  template: ''
})
class MockSampleDataFormComponent {
  @Input() sampleData: SampleDataModel;
}

@Component({
  selector: 'app-live-view-result',
  template: ''
})
class MockLiveViewResultComponent {
  @Input() results: EntryParsingResultModel[];
}

describe('LiveViewComponent', () => {
  let component: LiveViewComponent;
  let fixture: ComponentFixture<LiveViewComponent>;

  let mockStore: MockStore<{ 'live-view': LiveViewState }>;

  const initialState = { 'live-view': {
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

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        NzTabsModule,
        NzSpinModule,
        NzSwitchModule,
        FormsModule,
        RouterTestingModule
      ],
      providers: [
        provideMockStore({ initialState }),
      ],
      declarations: [
        LiveViewComponent,
        MockSampleDataFormComponent,
        MockLiveViewResultComponent,
      ]
    })
    .compileComponents();

    mockStore = TestBed.get(Store);
    spyOn(mockStore, 'dispatch').and.callThrough();
  }));

  beforeEach(() => {
    spyOn(localStorage, 'getItem');

    fixture = TestBed.createComponent(LiveViewComponent);
    component = fixture.componentInstance;
    component.chainConfig$ = new Subject<{}>();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch liveViewInitialized on init', () => {
    expect(mockStore.dispatch).toHaveBeenCalledWith({
      type: liveViewInitialized.type
    });
  });

  it('should react on sampleData change', fakeAsync(() => {
    component.sampleDataChange$.next(testSampleData);

    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE);

    expect(mockStore.dispatch).toHaveBeenCalledWith({
      sampleData: testSampleData,
      type: sampleDataInputChanged.type
    });
  }));

  it('should react on chain config change', fakeAsync(() => {
    mockStore.setState({ 'live-view': {
      ...initialState['live-view'],
      sampleData: testSampleData,
    }});
    fixture.detectChanges();

    (component.chainConfig$ as Subject<{}>).next({});
    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE);

    expect(mockStore.dispatch).toHaveBeenCalledWith({
      sampleData: testSampleData,
      chainConfig: {},
      type: executionTriggered.type
    });
  }));

  it('should filter out events without sample data input', fakeAsync(() => {
    (component.chainConfig$ as Subject<{}>).next({});
    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE);

    expect(mockStore.dispatch).not.toHaveBeenCalledWith({
      sampleData: testSampleData,
      chainConfig: {},
      type: executionTriggered.type
    });
  }));

  it('should hold back (debounce) executeTriggered', fakeAsync(() => {
    mockStore.setState({ 'live-view': {
      ...initialState['live-view'],
      sampleData: testSampleData,
    }});

    (component.chainConfig$ as Subject<{}>).next({});

    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE / 2);

    expect(mockStore.dispatch).not.toHaveBeenCalledWith({
      sampleData: testSampleData,
      chainConfig: {},
      type: executionTriggered.type
    });

    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE / 2);

    expect(mockStore.dispatch).toHaveBeenCalledWith({
      sampleData: testSampleData,
      chainConfig: {},
      type: executionTriggered.type
    });
  }));

  it('should trigger onOffToggleChanged if toggle change', () => {
    component.featureToggleChange$.next(true);

    expect(mockStore.dispatch).toHaveBeenCalledWith({
      value: true,
      type: onOffToggleChanged.type
    });
  });

  it('should persist selected tab on change', fakeAsync(() => {
    spyOn(localStorage, 'setItem');

    fixture.debugElement.query(By.css('.ant-tabs-tab:nth-child(2)')).nativeElement.click();
    fixture.detectChanges();

    tick(); // needed by the animation
    expect(localStorage.setItem).toHaveBeenCalledWith('liveViewSelectedTabIndex', '1');
  }));

  it('should restore selected tab on init', () => {
    expect(localStorage.getItem).toHaveBeenCalledWith('liveViewSelectedTabIndex');
  });

  it('should unsubscribe on destroy', fakeAsync(() => {
    component.ngOnDestroy();
    component.sampleDataChange$.next(testSampleData);

    tick(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE);

    expect(mockStore.dispatch).not.toHaveBeenCalledWith({
      sampleData: testSampleData,
      chainConfig: {},
      type: executionTriggered.type
    });
  }));
});
