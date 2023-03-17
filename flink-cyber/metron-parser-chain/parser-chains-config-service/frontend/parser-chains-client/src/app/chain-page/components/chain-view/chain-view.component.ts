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

import {ParserModel, PartialParserModel} from '../../chain-page.models';

import {ChainPageService} from '../../../services/chain-page.service';
import {Observable} from "rxjs";

@Component({
  selector: 'app-chain-view',
  templateUrl: './chain-view.component.html',
  styleUrls: ['./chain-view.component.scss']
})
export class ChainViewComponent implements OnInit {

  @Input() parsers: any [];
  @Input() dirtyParsers: string[];
  @Input() chainId: string;
  @Input() failedParser: Observable<string>;
  @Input() indexingFieldMap: Map<string,boolean>;
  @Output() removeParserEmitter = new EventEmitter<string>();
  @Output() chainLevelChange = new EventEmitter<string>();
  @Output() parserChange = new EventEmitter<PartialParserModel>();
  collapseAll: boolean;
  parserCollapseStateArray: boolean[];
  constructor(public chainPageService: ChainPageService) {
  }
  ngOnInit() {
    this.chainPageService.collapseAll.subscribe(value => this.collapseAll = value);
    this.chainPageService.createChainCollapseArray(this.parsers.length);
    this.chainPageService.getCollapseExpandState().subscribe(stateArray => {
      this.parserCollapseStateArray = stateArray;
    });
  }
  removeParser(id: string) {
    this.removeParserEmitter.emit(id);
  }

  onChainSelected(chainId: string) {
    this.chainLevelChange.emit(chainId);
  }

  onParserChange(parser: PartialParserModel) {
    this.parserChange.emit(parser);
  }

  onParserRemove(parserId: string) {
    this.removeParserEmitter.emit(parserId);
  }

  trackByFn(index: number, parser: any): string {
    return  (<ParserModel>parser).id !== undefined ? parser.id : parser;
  }

  collapseExpandAllParsers() {
    this.chainPageService.collapseExpandAllParsers();
  }

}
