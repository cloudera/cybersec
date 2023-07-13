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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { saveAs } from 'file-saver';
import {Clipboard} from '@angular/cdk/clipboard';

import { EntryParsingResultModel } from '../models/live-view.model';

@Component({
  selector: 'app-live-view-result',
  templateUrl: './live-view-result.component.html',
  styleUrls: ['./live-view-result.component.scss']
})
export class LiveViewResultComponent {
  @Input() results: EntryParsingResultModel[];
  @Output() investigateParserAction = new EventEmitter<string>();
  parserByParserViewId = null;


  constructor(private clipboard: Clipboard) {
  }

  onInvestigateParserClicked(failedParser) {
    this.investigateParserAction.emit(failedParser);
  }

  enableParserByParserView(entry) {
    this.parserByParserViewId = entry;
  }

  downloadEntries() {
    const outputArray = [];
    this.results.map((result) => {
      outputArray.push(JSON.stringify(result.output));
    });
    const outputBlob = new Blob([`[${outputArray}]`], {type: 'application/json;charset=utf-8'});
    saveAs(outputBlob, `results-${Date.now()}.json`);
  }

  copyOutput() {
    let output = JSON.stringify(this.results[this.results.length-1].output);
    this.clipboard.copy(output);
  }
}
