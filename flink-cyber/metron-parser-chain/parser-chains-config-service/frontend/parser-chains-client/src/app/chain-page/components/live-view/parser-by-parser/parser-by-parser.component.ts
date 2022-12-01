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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { ParserResultsModel } from '../models/live-view.model';

@Component({
  selector: 'app-parser-by-parser',
  templateUrl: './parser-by-parser.component.html',
  styleUrls: ['./parser-by-parser.component.scss']
})
export class ParserByParserComponent implements OnInit {
  @Input() parserResults: ParserResultsModel[];
  @Input() logMessage: string;
  @Output() investigateParser = new EventEmitter<string>();

  compileErrorDescription =
    'There was an error that prevented your parser chain from being constructed. Please review your configuration settings.';

  constructor() { }

  ngOnInit() {
  }

  enableInvestigateParser(parserId) {
    this.investigateParser.emit(parserId);
  }

}
