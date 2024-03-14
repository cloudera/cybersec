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

import {ParserFieldStatus, ParserResultsModel} from '../models/live-view.model';
import {Store} from "@ngrx/store";
import {DiffPopupState} from "../diff-popup/diff-popup.reducers";
import {ShowDiffModalAction} from "../diff-popup/diff-popup.actions";

@Component({
  selector: 'app-parser-by-parser',
  templateUrl: './parser-by-parser.component.html',
  styleUrls: ['./parser-by-parser.component.scss']
})
export class ParserByParserComponent implements OnInit {
  @Input() logMessage: string;
  @Output() investigateParser = new EventEmitter<string>();
  compileErrorDescription =
    'There was an error that prevented your parser chain from being constructed. Please review your configuration settings.';
  private _diffOnly: boolean = false;
  private _parserResults: ParserResultsModel[];
  private _calculatedParserResults: ParserResultsModel[];

  constructor(private store: Store<DiffPopupState>) {
  }

  ngOnInit() {
    this.updateParserResults()
  }

  @Input()
  set parserResults(value: ParserResultsModel[]) {
    this._parserResults = value;
    this.updateParserResults();
  }

  get parserResults(): ParserResultsModel[] {
    return this._calculatedParserResults;
  }

  get diffOnly(): boolean {
    return this._diffOnly;
  }

  set diffOnly(value: boolean) {
    this._diffOnly = value;
    this.updateParserResults();
  }

  private updateParserResults() {
    if (this.diffOnly){
      this._calculatedParserResults = this.calculateParsersDiff();
    } else {
      this._calculatedParserResults = this._parserResults
    }
  }

  showDiff(oldValue: string, newValue: string) {
    this.store.dispatch(ShowDiffModalAction({previousDiffValue: oldValue, newDiffValue: newValue}));
  }

  enableInvestigateParser(parserId) {
    this.investigateParser.emit(parserId);
  }

  private calculateParsersDiff() {
    let parserDiffs: ParserResultsModel[] = []
    let previous: ParserResultsModel = null

    for (const res of this._parserResults) {
      let diff = {}
      let prevOutput = previous ? previous['output'] : {};
      let currOutput = res['output'];

      for (const outputKey in currOutput) {
        let status: ParserFieldStatus
        let prevValue = prevOutput[outputKey];
        let currValue = currOutput[outputKey];
        if (prevValue){
          status = ParserFieldStatus.DIFF
        } else {
          status = ParserFieldStatus.NEW
        }
        if (prevValue != currValue){
          diff[outputKey] = {currentValue: currValue, previousValue: prevValue, status: status}
        }
      }
      if (prevOutput){
        for (const outputKey in prevOutput) {
          if (!currOutput[outputKey]) {
            diff[outputKey] = {currentValue: prevOutput[outputKey], status: ParserFieldStatus.REMOVED}
          }
        }
      }
      parserDiffs.push(
          {
            ...res,
            output: diff
          })
      previous = res
    }
    return parserDiffs
  }

  protected readonly ParserFieldStatus = ParserFieldStatus;

  protected readonly console = console;
}
