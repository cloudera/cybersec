import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Action } from '@ngrx/store';
import { NzMessageService } from 'ng-zorro-antd/message';
import { Observable, of, Subject, throwError } from 'rxjs';

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
import { LiveViewConsts } from './live-view.consts';
import { LiveViewEffects } from './live-view.effects';
import { SampleDataModel, SampleDataType } from './models/sample-data.model';
import { LiveViewService } from './services/live-view.service';

class MockLiveViewService {
  execute(sampleData: SampleDataModel, chainConfig: {}) {
    return new Subject();
  }
}

class MockMessageService {
  create(type: string, message: string) {}
}

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

  const testResult = {
    entries: [
      {
        output: 'output result',
        log: { type: '', message: 'log result'},
      }
    ]
  };

  const actions$ = new Subject<Action>();
  let liveViewEffects: LiveViewEffects;
  let fakeLiveViewService: LiveViewService;
  let fakeMessageService: NzMessageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        LiveViewEffects,
        { provide: LiveViewService, useClass: MockLiveViewService },
        { provide: NzMessageService, useClass: MockMessageService },

        provideMockActions(() => actions$)],
    });

    liveViewEffects = TestBed.get(LiveViewEffects);
    fakeLiveViewService = TestBed.get(LiveViewService);
    fakeMessageService = TestBed.get(NzMessageService);
  });

  it('should call liveViewService.execute on executionTriggered', () => {
    const testSubscriber = jasmine.createSpy('executionTriggeredSpy');
    liveViewEffects.execute$.subscribe(testSubscriber);

    spyOn(fakeLiveViewService, 'execute').and.returnValue(Observable.create({results: testResult}));

    actions$.next(executionTriggered({ ...testPayload }));

    expect(fakeLiveViewService.execute).toHaveBeenCalledWith(testPayload.sampleData, testPayload.chainConfig);
  });

  it('should dispatch liveViewRefreshedSuccessfully if liveViewService execute successfully', () => {
    const testSubscriber = jasmine.createSpy('executionTriggeredSpy');
    liveViewEffects.execute$.subscribe(testSubscriber);

    spyOn(fakeLiveViewService, 'execute').and.returnValue(of({
      ...testPayload,
      result: testResult,
    }));

    actions$.next(executionTriggered({ ...testPayload }));

    expect(testSubscriber).toHaveBeenCalledWith({
      liveViewResult: {
        ...testPayload,
        result: testResult,
      },
      type: liveViewRefreshedSuccessfully.type
    });
  });

  it('should dispatch liveViewRefreshFailed if liveViewService execution fail', () => {
    const testSubscriber = jasmine.createSpy('executionTriggeredSpy');
    liveViewEffects.execute$.subscribe(testSubscriber);

    spyOn(fakeLiveViewService, 'execute').and.returnValue(throwError({ message: 'something went wrong' }));

    actions$.next(executionTriggered({ ...testPayload }));

    expect(testSubscriber).toHaveBeenCalledWith({
      error: {
        message: 'something went wrong',
      },
      type: liveViewRefreshFailed.type
    });
  });

  it('should show error message if liveViewService execution fail', () => {
    spyOn(fakeLiveViewService, 'execute').and.returnValue(throwError({ message: 'something went wrong' }));
    spyOn(fakeMessageService, 'create');

    liveViewEffects.execute$.subscribe();

    actions$.next(executionTriggered({ ...testPayload }));

    expect(fakeMessageService.create).toHaveBeenCalledWith('error', 'something went wrong');
  });

  it('should persist sample data input to local storage', () => {
    spyOn(localStorage, 'setItem');

    liveViewEffects.persistingSapmleData$.subscribe();

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
