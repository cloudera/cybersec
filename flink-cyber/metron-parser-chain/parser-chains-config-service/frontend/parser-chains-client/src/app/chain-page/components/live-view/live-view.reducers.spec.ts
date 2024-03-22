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

import {
  executionTriggered,
  liveViewRefreshedSuccessfully,
  liveViewRefreshFailed,
  onOffToggleChanged,
  onOffToggleRestored,
  sampleDataInputChanged,
  sampleDataRestored,
} from './live-view.actions';
import { LiveViewActionsType } from './live-view.actions';
import { initialState, reducer } from './live-view.reducers';
import { SampleDataType } from './models/sample-data.model';
import {TestBed, waitForAsync} from "@angular/core/testing";
import {NzTabsModule} from "ng-zorro-antd/tabs";
import {NzSpinModule} from "ng-zorro-antd/spin";
import {NzSwitchModule} from "ng-zorro-antd/switch";
import {NzFormModule} from "ng-zorro-antd/form";
import {FormsModule} from "@angular/forms";
import {RouterTestingModule} from "@angular/router/testing";
import {provideMockStore} from "@ngrx/store/testing";
import {LiveViewComponent} from "./live-view.component";
import {MockComponent} from "ng-mocks";
import {SampleDataFormComponent} from "./sample-data-form/sample-data-form.component";
import {LiveViewResultComponent} from "./live-view-result/live-view-result.component";
import {CUSTOM_ELEMENTS_SCHEMA} from "@angular/core";

describe('live-view.reducers', () => {
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
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    })
      .compileComponents();
  }));

  const testConfigState = {
    id: '123',
    name: 'abcd',
    parsers: []
  };

  const testLiveViewState = {
    sampleData: {
      type: SampleDataType.MANUAL,
      source: '',
    },
    isExecuting: false,
    results: [],
  };

  it('should handle default case', () => {
    expect(reducer(undefined, { type: undefined } as LiveViewActionsType)).toBe(initialState);
  });

  it('should update isExecuting on executionTriggered action', () => {
    const newState = reducer(initialState, executionTriggered({ sampleData: testLiveViewState.sampleData, chainConfig: testConfigState }));
    expect(newState.isExecuting).toBe(true);
  });

  it('should update sampleData on executionTriggered action', () => {
    const newState = reducer(initialState, executionTriggered({ sampleData: testLiveViewState.sampleData, chainConfig: testConfigState }));
    expect(newState.isExecuting).toBe(true);
  });

  it('should update isExecuting on liveViewRefreshedSuccessfully action', () => {
    const results = [
      {
        output: 'output result',
        log: {
          type: '',
          message: 'log result',
          stackTrace: 'Fake Strack Trace Msg',
        },
      }
    ];
    const newState = reducer(initialState, liveViewRefreshedSuccessfully({ liveViewResult: {
      ...testLiveViewState,
      results
    } }));
    expect(newState.isExecuting).toBe(false);
  });

  it('should update result on liveViewRefreshedSuccessfully action', () => {
    const results = [
      {
        output: 'output result',
        log: {
          type: '',
          message: 'log result',
          stackTrace: 'Fake Strack Trace Msg',
        },
      }
    ];

    const newState = reducer(initialState, liveViewRefreshedSuccessfully({ liveViewResult: {
      ...testLiveViewState,
      results,
    } }));

    expect(newState.result).toEqual(results);
  });

  it('should update isExecuting on liveViewRefreshFailed action', () => {
    const newState = reducer(initialState, liveViewRefreshFailed({ error: { message: 'ups' } }));
    expect(newState.isExecuting).toBe(false);
  });

  it('should update isLiveViewOn on onOffToggleChanges action', () => {
    const newState = reducer(initialState, onOffToggleChanged({ value: true }));
    expect(newState.isLiveViewOn).toBe(true);
  });

  it('should update sampleData on sampleDataInputChanged action', () => {
    const newState = reducer(initialState, sampleDataInputChanged({
      sampleData: {
        type: SampleDataType.MANUAL,
        source: 'this just changed'
      }
    }));
    expect(newState.sampleData).toEqual({
      type: SampleDataType.MANUAL,
      source: 'this just changed'
    });
  });

  it('should update isLiveViewOn on onOffToggleRestored action', () => {
    const newState = reducer(initialState, onOffToggleRestored({ value: true }));
    expect(newState.isLiveViewOn).toBe(true);
  });

  it('should update sampleData on sampleDataRestored action', () => {
    const newState = reducer(initialState, sampleDataRestored({
      sampleData: {
        type: SampleDataType.MANUAL,
        source: 'this was persisted'
      }
    }));
    expect(newState.sampleData).toEqual({
      type: SampleDataType.MANUAL,
      source: 'this was persisted'
    });
  });

  it('should keep original isLiveViewOn stete when no state was persisted', () => {
    const newState = reducer(initialState, onOffToggleRestored({ value: null }));
    expect(newState.isLiveViewOn).toBe(initialState.isLiveViewOn);
  });

  it('should keep original sampleData stete when no state was persisted', () => {
    const newState = reducer(initialState, sampleDataRestored({
      sampleData: null
    }));
    expect(newState.sampleData).toEqual(initialState.sampleData);
  });

});
