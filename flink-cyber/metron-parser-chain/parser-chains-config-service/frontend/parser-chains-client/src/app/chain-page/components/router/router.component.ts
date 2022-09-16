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
