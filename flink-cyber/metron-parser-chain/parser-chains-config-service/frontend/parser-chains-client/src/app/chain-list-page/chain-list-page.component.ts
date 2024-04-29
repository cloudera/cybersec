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
import {UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators} from '@angular/forms';
import {select, Store} from '@ngrx/store';
import {BehaviorSubject, combineLatest, Observable, of} from 'rxjs';
import {switchMap, take} from 'rxjs/operators';

import * as fromActions from './chain-list-page.actions';
import {LoadPipelinesAction} from './chain-list-page.actions';
import {
  ChainListPageState,
  getChains,
  getCreateModalVisible,
  getDeleteChain,
  getDeleteModalVisible,
  getLoading,
  getPipelineRenameModalVisible,
  getPipelines,
  getSelectedPipeline,
} from './chain-list-page.reducers';
import {ChainModel, ChainOperationalModel} from './chain.model';
import {NzMessageService} from "ng-zorro-antd/message";

@Component({
  selector: 'app-chain-list-page',
  templateUrl: './chain-list-page.component.html',
  styleUrls: ['./chain-list-page.component.scss']
})
export class ChainListPageComponent implements OnInit {
  isChainCreateModalVisible$: Observable<boolean>;
  isPipelineRenameModalVisible$: Observable<boolean>;
  isOkLoading$: Observable<boolean>;
  chains$: Observable<ChainModel[]>;
  isChainDeleteModalVisible$: Observable<boolean>;
  deleteChainItem$: Observable<ChainModel>;
  pipelineList$: Observable<string[]>;
  totalRecords = 200;
  chainDataSorted$: Observable<ChainModel[]>;
  sortDescription$: BehaviorSubject<{ key: string, value: string }> = new BehaviorSubject({key: 'name', value: ''});
  newChainForm: UntypedFormGroup;
  renamePipelineForm: UntypedFormGroup;
  selectedPipeline$: Observable<string>;


  constructor(
    private _store: Store<ChainListPageState>,
    private _fb: UntypedFormBuilder,
    private _messageService: NzMessageService,
  ) {
    _store.dispatch(new LoadPipelinesAction());
    this.chains$ = _store.pipe(select(getChains));
    this.isOkLoading$ = _store.pipe(select(getLoading));
    this.isChainCreateModalVisible$ = _store.pipe(select(getCreateModalVisible));
    this.isChainDeleteModalVisible$ = _store.pipe(select(getDeleteModalVisible));
    this.isPipelineRenameModalVisible$ = _store.pipe(select(getPipelineRenameModalVisible));
    this.deleteChainItem$ = this._store.pipe(select(getDeleteChain));
    this.pipelineList$ = this._store.pipe(select(getPipelines));
    this.selectedPipeline$ = this._store.pipe(select(getSelectedPipeline));

    this.chainDataSorted$ = combineLatest([
      this.chains$,
      this.sortDescription$
    ]).pipe(
      switchMap(([chains, sortDescription]) => this.sortTable(chains, sortDescription))
    );
  }

  get chainName() {
    return this.newChainForm.get('chainName') as UntypedFormControl;
  }

  get newPipelineName() {
    return this.renamePipelineForm.get('pipelineName') as UntypedFormControl;
  }

  pipelineChanged($event: string) {
    this._store.dispatch(new fromActions.PipelineChangedAction($event))
  }

  showAddChainModal(): void {
    this._store.dispatch(new fromActions.ShowCreateModalAction());
  }

  showDeleteModal(id): void {
    this._store.dispatch(new fromActions.SelectDeleteChainAction(id));
  }

  pushChain(): void {
    const chainName = this.chainName.value;
    this.chains$.pipe(take(1)).subscribe(chainArr => {
      const duplicate = chainArr.some(value => {
        return value.name === chainName;
      });
      if (!duplicate) {
        const chainData: ChainOperationalModel = {name: chainName};
        this.newChainForm.reset();
        this._store.dispatch(new fromActions.CreateChainAction(chainData));
      } else {
        this._messageService.create('Error', "Duplicate chain names aren't allowed!");
      }
    })
  }

  deleteChain(chainId: string, chainName): void {
    this._store.dispatch(new fromActions.DeleteChainAction(chainId, chainName));
  }

  handleCancelChainModal(): void {
    this._store.dispatch(new fromActions.HideCreateModalAction());
  }

  handleCancelDeleteModal(): void {
    this._store.dispatch(new fromActions.HideDeleteModalAction());
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
    this.newChainForm = this._fb.group({
      chainName: new UntypedFormControl('', [Validators.required, Validators.minLength(3)]),
    });
    this.renamePipelineForm = this._fb.group({
      pipelineName: new UntypedFormControl('', [Validators.required, Validators.minLength(3)]),
    });
  }

  showPipelineRenameModal() {
    this._store.dispatch(new fromActions.ShowRenameSelectedPipelineModalAction());
  }

  handlePipelineRenameModalCancel() {
    this._store.dispatch(new fromActions.HideRenamePipelineModalAction());
  }

  createPipeline(inputElement: HTMLInputElement) {
    const pipelineName = inputElement.value;
    this._store.dispatch(new fromActions.CreatePipelineAction(pipelineName));
  }

  deletePipeline() {
    this._store.dispatch(new fromActions.DeleteSelectedPipelineAction());
  }

  renamePipeline() {
    const newPipelineName = this.newPipelineName.value;
    this.renamePipelineForm.reset();
    this._store.dispatch(new fromActions.RenameSelectedPipelineAction(newPipelineName));
  }
}
