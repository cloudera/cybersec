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

import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {select, Store} from '@ngrx/store';
import {BehaviorSubject, combineLatest, Observable, of} from 'rxjs';
import {switchMap, take} from 'rxjs/operators';

import * as fromActions from './chain-list-page.actions';
import {LoadChainsAction, LoadPipelinesAction} from './chain-list-page.actions';
import {
  ChainListPageState,
  getChains,
  getCreateModalVisible, getCurrentPipeline, getDeleteChain,
  getDeleteModalVisible,
  getLoading, getPipelines,
} from './chain-list-page.reducers';
import {ChainModel, ChainOperationalModel, PipelineModel} from './chain.model';
import {NzMessageService} from "ng-zorro-antd/message";

@Component({
  selector: 'app-chain-list-page',
  templateUrl: './chain-list-page.component.html',
  styleUrls: ['./chain-list-page.component.scss']
})
export class ChainListPageComponent implements OnInit {
  isChainCreateModalVisible$: Observable<boolean>;
  isOkLoading$: Observable<boolean>;
  chains$: Observable<ChainModel[]>;
  isChainDeleteModalVisible$: Observable<boolean>;
  deleteChainItem$: Observable<ChainModel>;
  pipelineList$: Observable<PipelineModel[]>;
  totalRecords = 200;
  chainDataSorted$: Observable<ChainModel[]>;
  sortDescription$: BehaviorSubject<{ key: string, value: string }> = new BehaviorSubject({key: 'name', value: ''});
  newChainForm: FormGroup;
  private _selectedPipeline: PipelineModel;

  get selectedPipeline(): PipelineModel {
    let value;
    this.store.pipe(select(getCurrentPipeline)).pipe(take(1)).subscribe(v => value = v);
    return value;
  }

  set selectedPipeline(value: PipelineModel) {
    this._selectedPipeline = value;
  }


  constructor(
    private store: Store<ChainListPageState>,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private messageService: NzMessageService,
  ) {
    store.dispatch(new LoadPipelinesAction());
    store.dispatch(new LoadChainsAction());
    this.chains$ = store.pipe(select(getChains));
    this.isOkLoading$ = store.pipe(select(getLoading));
    this.isChainCreateModalVisible$ = store.pipe(select(getCreateModalVisible));
    this.isChainDeleteModalVisible$ = store.pipe(select(getDeleteModalVisible));
    this.deleteChainItem$ = this.store.pipe(select(getDeleteChain));
    this.pipelineList$ = this.store.pipe(select(getPipelines));

    this.chainDataSorted$ = combineLatest([
      this.chains$,
      this.sortDescription$
    ]).pipe(
      switchMap(([chains, sortDescription]) => this.sortTable(chains, sortDescription))
    );
  }

  pipelineChanged($event: PipelineModel) {
    this.store.dispatch(new fromActions.PipelineChangedAction($event))
    this.store.dispatch(new LoadChainsAction());
  }

  get chainName() {
    return this.newChainForm.get('chainName') as FormControl;
  }

  showAddChainModal(): void {
    this.store.dispatch(new fromActions.ShowCreateModalAction());
  }

  showDeleteModal(id): void {
    this.store.dispatch(new fromActions.SelectDeleteChainAction(id));
  }

  pushChain(): void {
    let chainName = this.chainName.value;
    this.chains$.pipe(take(1)).subscribe(chainArr => {
      const duplicate = chainArr.some(value => {
        return value.name == chainName;
      });
      if (!duplicate) {
        const chainData: ChainOperationalModel = {name: chainName};
        this.newChainForm.reset();
        this.store.dispatch(new fromActions.CreateChainAction(chainData));
      } else {
        this.messageService.create('Error', "Duplicate chain names aren't allowed!");
      }
    })
  }

  deleteChain(chainId: string, chainName): void {
    this.store.dispatch(new fromActions.DeleteChainAction(chainId, chainName));
  }

  handleCancelChainModal(): void {
    this.store.dispatch(new fromActions.HideCreateModalAction());
  }

  handleCancelDeleteModal(): void {
    this.store.dispatch(new fromActions.HideDeleteModalAction());
  }

  sortTable(data: ChainModel[], sortDescription: any): Observable<ChainModel[]> {
    const sortValue = sortDescription.value;
    const newData = (data || []).slice().sort((a, b) => {
      const first = a.name.toLowerCase();
      const second = b.name.toLowerCase();
      if (sortValue === 'ascend') {
        return (first < second) ? -1 : 1;
      } else if (sortValue === 'descend') {
        return (first < second) ? 1 : -1;
      } else {
        return 0;
      }
    });
    return of(newData);
  }

  ngOnInit() {
    this.newChainForm = this.fb.group({
      chainName: new FormControl('', [Validators.required, Validators.minLength(3)]),
    });
  }
}
