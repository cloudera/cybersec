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

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { switchMap, take } from 'rxjs/operators';

import { LoadChainsAction } from './chain-list-page.actions';
import * as fromActions from './chain-list-page.actions';
import {
  ChainListPageState,
  getChains,
  getCreateModalVisible,
  getLoading,
} from './chain-list-page.reducers';
import { ChainModel, ChainOperationalModel } from './chain.model';
import {NzMessageService} from "ng-zorro-antd/message";

@Component({
  selector: 'app-chain-list-page',
  templateUrl: './chain-list-page.component.html',
  styleUrls: ['./chain-list-page.component.scss']
})
export class ChainListPageComponent implements OnInit {
  isChainCreateModalVisible$: Observable<boolean>;
  isVisibleDeleteModal: boolean = false;
  isOkLoading$: Observable<boolean>;
  chains$: Observable<ChainModel[]>;
  totalRecords = 200;
  chainDataSorted$: Observable<ChainModel[]>;
  sortDescription$: BehaviorSubject<{key: string, value: string}> = new BehaviorSubject({ key: 'name', value: '' });
  newChainForm: FormGroup;

  constructor(
    private store: Store<ChainListPageState>,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private messageService: NzMessageService,
  ) {
    this.route.queryParams.subscribe(() => {
      store.dispatch(new LoadChainsAction());
      this.chains$ = store.pipe(select(getChains));
      this.isOkLoading$ = store.pipe(select(getLoading));
      this.isChainCreateModalVisible$ = store.pipe(select(getCreateModalVisible));
    });

    this.chainDataSorted$ = combineLatest([
      this.chains$,
      this.sortDescription$
    ]).pipe(
      switchMap(([ chains, sortDescription ]) => this.sortTable(chains, sortDescription))
    );
  }

  get chainName() {
    return this.newChainForm.get('chainName') as FormControl;
  }

  showAddChainModal(): void {
    this.store.dispatch(new fromActions.ShowCreateModalAction());
  }

  showDeleteModal(): void {
    this.isVisibleDeleteModal = true;
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
    this.isVisibleDeleteModal = false;
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
