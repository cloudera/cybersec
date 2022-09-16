import { Component, EventEmitter, Input, Output } from '@angular/core';
import { saveAs } from 'file-saver';

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
}
