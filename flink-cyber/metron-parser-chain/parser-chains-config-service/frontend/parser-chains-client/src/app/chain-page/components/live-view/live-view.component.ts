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

import { AfterViewInit, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { combineLatest, Observable, Subject } from 'rxjs';
import { debounceTime, filter, takeUntil } from 'rxjs/operators';

import { InvestigateParserAction } from '../../chain-page.actions';

import {
  executionTriggered,
  liveViewInitialized,
  onOffToggleChanged,
  sampleDataInputChanged,
} from './live-view.actions';
import { LiveViewConsts } from './live-view.consts';
import { LiveViewState } from './live-view.reducers';
import {
  getExecutionStatus,
  getIsLiveViewOn,
  getResults,
  getSampleData,
} from './live-view.selectors';
import { EntryParsingResultModel } from './models/live-view.model';
import {SampleDataInternalModel, SampleDataModel} from './models/sample-data.model';
import {
  ExecutionListTriggeredAction
} from "./sample-data-form/sample-data-text-folder-input/sample-data-text-folder-input.actions";
import {ChainDetailsModel} from "../../chain-page.models";

@Component({
  selector: 'app-live-view',
  templateUrl: './live-view.component.html',
  styleUrls: ['./live-view.component.scss']
})
export class LiveViewComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() chainConfig$: Observable<ChainDetailsModel>;
  @Output() failedParser = new EventEmitter<string>();
  @Output() parserToBeInvestigated = new EventEmitter<string>();
  isLiveViewOn$: Observable<boolean>;
  isExecuting$: Observable<boolean>;
  results$: Observable<EntryParsingResultModel[]>;
  sampleData$: Observable<SampleDataModel>;
  sampleDataChange$ = new Subject<SampleDataModel>();
  sampleDataForceChange$ = new Subject<SampleDataInternalModel[]>();
  featureToggleChange$ = new Subject<boolean>();
  selectedTabIndex = 0;
  private _unsubscribe$: Subject<void> = new Subject<void>();

  constructor(private _store: Store<LiveViewState>) {
    this.isExecuting$ = this._store.pipe(select(getExecutionStatus));
    this.results$ = this._store.pipe(select(getResults));
    this.sampleData$ = this._store.pipe(select(getSampleData));
    this.isLiveViewOn$ = this._store.pipe(select(getIsLiveViewOn));
  }

  ngOnInit() {
    this._store.dispatch(liveViewInitialized());
    this.selectedTabIndex = this._restoreSelectedTab();
  }

  ngAfterViewInit() {
    this._subscribeToRelevantChanges();
  }

  onInvestigateParserAction(parserId) {
    this._store.dispatch(new InvestigateParserAction({ id: parserId }));
  }

  onSelectedTabChange(index: number) {
    this._persistSelectedTab(index);
  }

  ngOnDestroy(): void {
    this._unsubscribe$.next();
    this._unsubscribe$.complete();
  }

  private _subscribeToRelevantChanges() {
    combineLatest([
      this.sampleData$,
      this.chainConfig$,
      this.isLiveViewOn$,
    ]).pipe(
      debounceTime(LiveViewConsts.LIVE_VIEW_DEBOUNCE_RATE),
      filter(([ sampleData, chainConfig, isLiveViewOn ]) => isLiveViewOn && !!sampleData.source),
      takeUntil(this._unsubscribe$)
    ).subscribe(([ sampleData, chainConfig ]) => {
      this._store.dispatch(executionTriggered({ sampleData, chainConfig }));
    });

    this.featureToggleChange$.pipe(
      takeUntil(this._unsubscribe$),
      filter(value => value !== null),
    ).subscribe(value => {
      this._store.dispatch(onOffToggleChanged({ value }));
    });

    this.sampleDataChange$.pipe(
      takeUntil(this._unsubscribe$),
      filter(sampleData => sampleData !== null),
    ).subscribe(sampleData => {
      this._store.dispatch(sampleDataInputChanged({ sampleData }));
    });

    combineLatest([
      this.sampleDataForceChange$,
      this.chainConfig$]).pipe(
      takeUntil(this._unsubscribe$),
      filter((sampleData, chainConfig) => sampleData !== null),
    ).subscribe(([sampleData, chainConfig]) => {
      this._store.dispatch(ExecutionListTriggeredAction({ sampleData, chainConfig }));
    });
  }

  private _persistSelectedTab(index: number) {
    localStorage.setItem('liveViewSelectedTabIndex', JSON.stringify(index));
  }

  private _restoreSelectedTab() {
    return parseInt(localStorage.getItem('liveViewSelectedTabIndex'), 10);
  }
}
