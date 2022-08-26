import { Component, EventEmitter, Input, Output } from '@angular/core';
import { isEqual } from 'lodash';

export interface ConfigChangedEvent {
  value: {};
}

@Component({
  selector: 'app-advanced-editor',
  templateUrl: './advanced-editor.component.html',
  styleUrls: ['./advanced-editor.component.scss']
})
export class AdvancedEditorComponent {

  @Input() config = {};
  @Output() configChanged = new EventEmitter<ConfigChangedEvent>();

  monacoOptions = {
    language: 'json',
    glyphMargin: false,
    folding: false,
    lineDecorationsWidth: 10,
    lineNumbersMinChars: 0,
    minimap: {
      enabled: false
    },
    automaticLayout: true,
    formatOnPaste: true,
  };

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
