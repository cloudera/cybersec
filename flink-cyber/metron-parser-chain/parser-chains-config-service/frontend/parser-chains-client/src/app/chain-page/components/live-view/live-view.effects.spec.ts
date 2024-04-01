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

import {TestBed} from '@angular/core/testing';
import {provideMockActions} from '@ngrx/effects/testing';
import {Action} from '@ngrx/store';
import {NzMessageService} from 'ng-zorro-antd/message';
import {of, Subject, throwError} from 'rxjs';

import {
  executionTriggered,
  liveViewInitialized,
  liveViewRefreshedSuccessfully,
  liveViewRefreshFailed,
  onOffToggleChanged,
  onOffToggleRestored,
  sampleDataInputChanged,
  sampleDataRestored
} from './live-view.actions';
import {LiveViewConsts} from './live-view.consts';
import {LiveViewEffects} from './live-view.effects';
import {SampleDataType} from './models/sample-data.model';
import {LiveViewService} from './services/live-view.service';
import {EntryParsingResultModel} from "./models/live-view.model";

describe('live-view.effects', () => {

  const testPayload = {
    sampleData: {
      type: SampleDataType.MANUAL,
      source: 'test sample data',
    },
    chainConfig: {
      id: '123',
      name: 'abc',
      parsers: []
    }
  };

  const testResult:EntryParsingResultModel[] =
     [
      {
        output: 'output result',
        log: { type: '', message: 'log result', stackTrace: '' },
      }
    ];

  const actions$ = new Subject<Action>();
  let liveViewEffects: jasmine.SpyObj<LiveViewEffects>;
  let fakeLiveViewService: jasmine.SpyObj<LiveViewService>;
  let fakeMessageService: jasmine.SpyObj<NzMessageService>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        LiveViewEffects,
        { provide: LiveViewService, useValue: jasmine.createSpyObj('LiveViewService', {
            execute: of({results: testResult})
          }) },
        { provide: NzMessageService, useValue: jasmine.createSpyObj('NzMessageService', ['create']) },

        provideMockActions(() => actions$)],
    });

    liveViewEffects = TestBed.inject(LiveViewEffects) as jasmine.SpyObj<LiveViewEffects>;
    fakeLiveViewService = TestBed.inject(LiveViewService) as jasmine.SpyObj<LiveViewService>;
    fakeMessageService = TestBed.inject(NzMessageService) as jasmine.SpyObj<NzMessageService>;
  });

  it('should call liveViewService.execute on executionTriggered', () => {
    const testSubscriber = jasmine.createSpy('executionTriggeredSpy');
    liveViewEffects.execute$.subscribe(testSubscriber);

    actions$.next(executionTriggered({ ...testPayload }));

    expect(fakeLiveViewService.execute).toHaveBeenCalledWith(testPayload.sampleData, testPayload.chainConfig);
  });

  it('should dispatch liveViewRefreshedSuccessfully if liveViewService execute successfully', () => {
    const testSubscriber = jasmine.createSpy('executionTriggeredSpy');
    liveViewEffects.execute$.subscribe(testSubscriber);

    actions$.next(executionTriggered({ ...testPayload }));

    expect(testSubscriber).toHaveBeenCalledWith({
      liveViewResult: {
        results: testResult,
      },
      type: liveViewRefreshedSuccessfully.type
    });
  });

  it('should dispatch liveViewRefreshFailed if liveViewService execution fail', () => {
    const testSubscriber = jasmine.createSpy('executionTriggeredSpy');
    liveViewEffects.execute$.subscribe(testSubscriber);

    fakeLiveViewService.execute.and.returnValue(throwError({ message: 'something went wrong' }));

    actions$.next(executionTriggered({ ...testPayload }));

    expect(testSubscriber).toHaveBeenCalledWith({
      error: {
        message: 'something went wrong',
      },
      type: liveViewRefreshFailed.type
    });
  });

  it('should show error message if liveViewService execution fail', () => {
    fakeLiveViewService.execute.and.returnValue(throwError({ message: 'something went wrong' }));

    liveViewEffects.execute$.subscribe();

    actions$.next(executionTriggered({ ...testPayload }));

    expect(fakeMessageService.create).toHaveBeenCalledWith('error', 'something went wrong');
  });

  it('should persist sample data input to local storage', () => {
    spyOn(localStorage, 'setItem');

    liveViewEffects.persistingSampleData$.subscribe();

    actions$.next(sampleDataInputChanged({ sampleData: { type: SampleDataType.MANUAL, source: 'testing persistance' } }));

    expect(localStorage.setItem).toHaveBeenCalledWith(
      LiveViewConsts.SAMPLE_DATA_STORAGE_KEY,
      JSON.stringify({ type: SampleDataType.MANUAL, source: 'testing persistance' })
    );
  });

  it('should restore sample data input from local storage on liveViewInitialized action', () => {
    const testSubscriber = jasmine.createSpy('sampleDataRestoredSpy');
    liveViewEffects.restoreSampleDataFromLocalStore.subscribe(testSubscriber);

    spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify({ type: SampleDataType.MANUAL, source: 'persisted state' }));

    actions$.next(liveViewInitialized());

    expect(testSubscriber).toHaveBeenCalledWith({
      sampleData: { type: SampleDataType.MANUAL, source: 'persisted state' },
      type: sampleDataRestored.type
    });
  });

  it('should persist on/off toggle state to local storage', () => {
    spyOn(localStorage, 'setItem');

    liveViewEffects.persistingOnOffToggle$.subscribe();

    actions$.next(onOffToggleChanged({ value: true }));

    expect(localStorage.setItem).toHaveBeenCalledWith(
      LiveViewConsts.FEATURE_TOGGLE_STORAGE_KEY,
      'true'
    );
  });

  it('should restore on/off toggle state from local storage on liveViewInitialized action', () => {
    const testSubscriber = jasmine.createSpy('onOffToggleRestoredSpy');
    liveViewEffects.restoreToggleFromLocalStore.subscribe(testSubscriber);

    spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(true));

    actions$.next(liveViewInitialized());

    expect(testSubscriber).toHaveBeenCalledWith({
      value: true,
      type: onOffToggleRestored .type
    });
  });

});
