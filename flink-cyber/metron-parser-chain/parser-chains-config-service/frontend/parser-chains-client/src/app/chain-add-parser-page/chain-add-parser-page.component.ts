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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { v1 as uuidv1 } from 'uuid';

import * as fromParserPageAction from '../chain-page/chain-page.actions';
import { ParserModel, ParserChainModel } from '../chain-page/chain-page.models';
import { getChain } from '../chain-page/chain-page.reducers';

import * as fromActions from './chain-add-parser-page.actions';
import { AddParserPageState, getParserTypes } from './chain-add-parser-page.reducers';

@Component({
  selector: 'app-chain-add-parser-page',
  templateUrl: './chain-add-parser-page.component.html',
  styleUrls: ['./chain-add-parser-page.component.scss']
})
export class ChainAddParserPageComponent implements OnInit, OnDestroy {
  addParserForm: UntypedFormGroup;
  typesList: { id: string, name: string }[] = [];
  parsersList: ParserModel[] = [];
  chainId: string;
  subchainId: string;
  getChainSubscription: Subscription;
  getParserTypesSubscription: Subscription;

  constructor(
    private _fb: UntypedFormBuilder,
    private _store: Store<AddParserPageState>,
    private _activatedRoute: ActivatedRoute,
    private _router: Router
  ) {}

  get name() {
    return this.addParserForm.get('name') as UntypedFormControl;
  }
  get type() {
    return this.addParserForm.get('type') as UntypedFormControl;
  }
  get sourceParser() {
    return this.addParserForm.get('chainId') as UntypedFormControl;
  }

  addParser() {
    const parser = {
      ...this.addParserForm.value,
      id: uuidv1(),
      config: {},
    };
    if (this.addParserForm.value.type === 'Router') {
      parser.routing = {};
    }
    this._store.dispatch(new fromActions.AddParserAction({
      chainId: this.subchainId || this.chainId,
      parser
    }));

    this._router.navigateByUrl(`/parserconfig/chains/${this.chainId}`);
  }

  ngOnInit() {
    this.addParserForm = this._fb.group({
      name: new UntypedFormControl('', [Validators.required, Validators.minLength(3)]),
      type: new UntypedFormControl(null)
    });

    this._activatedRoute.params.subscribe((params) => {
      this.chainId = params.id;
      this.subchainId = params.subchain;
    });

    this.getChainSubscription = this._store.pipe(select(getChain({ id: this.chainId }))).subscribe((chain: ParserChainModel) => {
      if (!chain) {
        this._store.dispatch(new fromParserPageAction.LoadChainDetailsAction({
          id: this.chainId
        }));
      }
    });

    this._store.dispatch(new fromActions.GetParserTypesAction());

    this.getParserTypesSubscription = this._store.pipe(select(getParserTypes)).subscribe((parserTypes) => {
      if (parserTypes !== undefined) {
        this.typesList = [...parserTypes];
        this.typesList.sort((a, b) => a.name.toUpperCase() < b.name.toUpperCase() ? -1 : 1);
      }
    });
  }

  ngOnDestroy() {
    if (this.getChainSubscription) {
      this.getChainSubscription.unsubscribe();
    }
    if (this.getParserTypesSubscription) {
      this.getParserTypesSubscription.unsubscribe();
    }
  }
}
