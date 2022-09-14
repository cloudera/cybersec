import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import * as uuid from 'uuid';
import * as fromParserPageAction from '../parsers/parsers.actions';
import { ParserModel, ParserChainModel } from '../parsers/parsers.models';
import { getChain } from '../parsers/parsers.reducers';

import * as fromActions from './chain-add-parser-page.actions';
import { AddParserPageState, getParserTypes } from './chain-add-parser-page.reducers';

@Component({
  selector: 'app-chain-add-parser-page',
  templateUrl: './chain-add-parser-page.component.html',
  styleUrls: ['./chain-add-parser-page.component.scss']
})
export class ChainAddParserPageComponent implements OnInit, OnDestroy {
  addParserForm: FormGroup;
  typesList: { id: string, name: string }[] = [];
  parsersList: ParserModel[] = [];
  chainId: string;
  subchainId: string;
  getChainSubscription: Subscription;
  getParserTypesSubscription: Subscription;

  constructor(
    private fb: FormBuilder,
    private store: Store<AddParserPageState>,
    private activatedRoute: ActivatedRoute,
    private router: Router
  ) {}

  get name() {
    return this.addParserForm.get('name') as FormControl;
  }
  get type() {
    return this.addParserForm.get('type') as FormControl;
  }
  get sourceParser() {
    return this.addParserForm.get('chainId') as FormControl;
  }

  addParser() {
    const parser = {
      ...this.addParserForm.value,
      id: uuid.v1(),
      config: {},
    };
    if (this.addParserForm.value.type === 'Router') {
      parser.routing = {};
    }
    this.store.dispatch(new fromActions.AddParserAction({
      chainId: this.subchainId || this.chainId,
      parser
    }));

    this.router.navigateByUrl(`/parserconfig/chains/${this.chainId}`);
  }

  ngOnInit() {
    this.addParserForm = this.fb.group({
      name: new FormControl('', [Validators.required, Validators.minLength(3)]),
      type: new FormControl(null)
    });

    this.activatedRoute.params.subscribe((params) => {
      this.chainId = params.id;
      this.subchainId = params.subchain;
    });

    this.getChainSubscription = this.store.pipe(select(getChain, { id: this.chainId })).subscribe((chain: ParserChainModel) => {
      if (!chain) {
        this.store.dispatch(new fromParserPageAction.LoadChainDetailsAction({
          id: this.chainId
        }));
      }
    });

    this.store.dispatch(new fromActions.GetParserTypesAction());

    this.getParserTypesSubscription = this.store.pipe(select(getParserTypes)).subscribe((parserTypes) => {
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
