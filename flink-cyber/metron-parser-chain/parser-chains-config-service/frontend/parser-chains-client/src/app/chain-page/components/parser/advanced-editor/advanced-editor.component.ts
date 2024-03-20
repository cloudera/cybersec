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

import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {isEqual} from 'lodash';

export interface ConfigChangedEvent {
  value: {};
}

@Component({
  selector: 'app-advanced-editor',
  templateUrl: './advanced-editor.component.html',
  styleUrls: ['./advanced-editor.component.scss']
})
export class AdvancedEditorComponent implements OnChanges {

  @Input() config = {};
  @Input() isReadOnly: boolean = false;
  @Output() configChanged = new EventEmitter<ConfigChangedEvent>();

  monacoOptions = {
    language: 'json',
    glyphMargin: false,
    folding: false,
    lineDecorationsWidth: 10,
    lineNumbersMinChars: 0,
    readOnly: this.isReadOnly,
    minimap: {
      enabled: false
    },
    automaticLayout: true,
    formatOnPaste: true,
    scrollBeyondLastLine: false
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.isReadOnly !== undefined){
      this.monacoOptions["readOnly"]= this.isReadOnly;
    }
  }

  onChange(value: string) {
    let json = {};

    try {
      json = JSON.parse(value);
    } catch {
      return;
    }

    if (!isEqual(json, this.config)) {
      this.configChanged.emit({ value: json });
    }
  }

}
