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
import {getObjProp} from "../../../../shared/utils";

@Component({
  selector: 'app-parser-by-parser',
  templateUrl: './parser-by-parser.component.html',
  styleUrls: ['./parser-by-parser.component.scss']
})
export class ParserByParserComponent implements OnInit {
  static readonly ERROR_DESCRIPTOR =
    'There was an error that prevented your parser chain from being constructed. Please review your configuration settings.';
  @Output() investigateParser = new EventEmitter<string>();
  @Input() logMessage: string;
  errorDescriptor = ParserByParserComponent.ERROR_DESCRIPTOR;
  protected readonly parserFieldStatus = ParserFieldStatus;
  protected readonly console = console;
  private _diffOnly = false;
  private _parserResults: ParserResultsModel[];
  private _calculatedParserResults: ParserResultsModel[];

  constructor(private _store: Store<DiffPopupState>) {
  }
  get diffOnly(): boolean {
    return this._diffOnly;
  }
  get parserResults(): ParserResultsModel[] {
    return this._calculatedParserResults;
  }
  @Input()
  set parserResults(value: ParserResultsModel[]) {
    this._parserResults = value;
    this._updateParserResults();
  }
  set diffOnly(value: boolean) {
    this._diffOnly = value;
    this._updateParserResults();
  }


  ngOnInit() {
    this._updateParserResults()
  }


  showDiff(oldValue: string, newValue: string) {
    this._store.dispatch(ShowDiffModalAction({previousDiffValue: oldValue, newDiffValue: newValue}));
  }

  enableInvestigateParser(parserId) {
    this.investigateParser.emit(parserId);
  }



  private _calculateParsersDiff() {
    const parserDiffs: ParserResultsModel[] = []
    let previous: ParserResultsModel = null

    for (const res of this._parserResults) {
      const diff = {}
      const prevOutput = previous ? previous.output : {};
      const currOutput = res.output;

      Object.keys(currOutput).forEach((outputKey) => {
        let status: ParserFieldStatus

        const prevValue = getObjProp(prevOutput, outputKey);
        const currValue = getObjProp(currOutput, outputKey);
        if (prevValue){
          status = ParserFieldStatus.DIFF
        } else {
          status = ParserFieldStatus.NEW
        }
        if (prevValue !== currValue){
          diff[outputKey] = {currentValue: currValue, previousValue: prevValue, status}
        }
      });
      if (prevOutput){
        for (const outputKey in prevOutput) {
          if (!getObjProp(currOutput, outputKey)) {
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
  private _updateParserResults() {
    if (this.diffOnly){
      this._calculatedParserResults = this._calculateParsersDiff();
    } else {
      this._calculatedParserResults = this._parserResults
    }
  }
}
