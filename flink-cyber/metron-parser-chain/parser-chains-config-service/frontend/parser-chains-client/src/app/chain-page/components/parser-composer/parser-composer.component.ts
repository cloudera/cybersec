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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {select, Store} from '@ngrx/store';

import {v1 as uuidv1} from 'uuid';

import * as fromActions from '../../chain-page.actions';
import {ParserModel, PartialParserModel} from '../../chain-page.models';
import {ChainPageState, getFormConfigByType, getParser, getParserToBeInvestigated} from '../../chain-page.reducers';
import {CustomFormConfig} from '../custom-form/custom-form.component';

@Component({
  selector: 'app-parser-composer',
  templateUrl: './parser-composer.component.html',
  styleUrls: ['./parser-composer.component.scss']
})
export class ParserComposerComponent implements OnInit {

  @Input() collapsed: boolean;
  @Input() dirty = false;
  @Input() parserId: string;
  @Input() chainId: string;
  @Input() failedParser: string;
  @Input() selectedSource: string;
  @Input() indexingFieldMap: Map<string, Map<string, boolean>>;
  @Output() subchainSelect = new EventEmitter<string>();
  @Output() parserRemove = new EventEmitter<string>();
  @Output() parserChange = new EventEmitter<PartialParserModel>();

  configForm: CustomFormConfig[];
  parserType: string;
  parser: ParserModel;
  isolatedParserView = false;

  constructor(
    private store: Store<ChainPageState>
  ) {
  }

  ngOnInit() {
    this.store.pipe(select(getParser({
      id: this.parserId
    }))).subscribe((parser) => {
      this.parser = parser;
      if (parser) {
        this.store.pipe(select(getFormConfigByType({type: parser.type})))
          .subscribe((formConfig: CustomFormConfig[]) => {
            if (formConfig) {
              this.configForm = formConfig;
              this.parserType = parser.type;
            }
          });
      }
    });

    this.store
      .pipe(select(getParserToBeInvestigated))
      .subscribe(id => this.isolatedParserView = !!id);
  }

  onSubchainSelect(chainId: string) {
    this.subchainSelect.emit(chainId);
  }

  onParserChange(partialParser) {
    this.store.dispatch(
      new fromActions.UpdateParserAction({
        chainId: this.chainId,
        parser: partialParser
      })
    );
    this.parserChange.emit(partialParser);
  }

  onRemoveParser(parserId: string) {
    this.parserRemove.emit(parserId);
  }

  onRouteAdd(parser: ParserModel) {
    const chainId = uuidv1();
    const routeId = uuidv1();
    this.store.dispatch(
      new fromActions.AddChainAction({
        chain: {
          id: chainId,
          name: chainId
        }
      })
    );
    this.store.dispatch(
      new fromActions.AddRouteAction({
        chainId,
        parserId: parser.id,
        route: {
          id: routeId,
          name: routeId,
          subchain: chainId,
          default: false,
        }
      })
    );
  }
}
