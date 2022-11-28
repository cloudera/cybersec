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

import { ParserModel, PartialParserModel } from '../../chain-page.models';
import { ParserComponent } from '../parser/parser.component';

@Component({
  selector: 'app-router',
  templateUrl: './router.component.html',
  styleUrls: ['./router.component.scss']
})
export class RouterComponent extends ParserComponent {

  @Input() collapsed: boolean;
  @Input() dirty = false;
  @Input() parser: ParserModel;
  @Output() subchainSelect = new EventEmitter<string>();
  @Output() routeAdd = new EventEmitter<ParserModel>();
  @Output() parserChange = new EventEmitter<PartialParserModel>();

  onMatchingFieldBlur(event: Event, parser: ParserModel) {
    const matchingField = ((event.target as HTMLInputElement).value || '').trim();
    this.parserChange.emit({
      id: parser.id,
      routing: {
        ...(parser.routing || {}),
        matchingField
      }
    });
  }

  onSubchainClick(chainId: string) {
    this.subchainSelect.emit(chainId);
  }

  onAddRouteClick(event: Event, parser: ParserModel) {
    event.preventDefault();
    this.routeAdd.emit(parser);
  }

  trackByFn(index, routeId) {
    return routeId;
  }
}
