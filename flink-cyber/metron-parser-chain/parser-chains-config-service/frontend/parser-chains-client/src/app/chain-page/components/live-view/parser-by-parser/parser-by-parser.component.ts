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
