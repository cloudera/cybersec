import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { ParserModel, PartialParserModel } from '../../chain-page.models';

import { ChainPageService } from './../../../services/chain-page.service';

@Component({
  selector: 'app-chain-view',
  templateUrl: './chain-view.component.html',
  styleUrls: ['./chain-view.component.scss']
})
export class ChainViewComponent implements OnInit {

  @Input() parsers: ParserModel[];
  @Input() dirtyParsers: string[];
  @Input() chainId: string;
  @Input() failedParser: string;
  @Output() removeParserEmitter = new EventEmitter<string>();
  @Output() chainLevelChange = new EventEmitter<string>();
  @Output() parserChange = new EventEmitter<PartialParserModel>();
  collapseAll: boolean;
  parserCollapseStateArray: boolean[];
  constructor(public chainPageService: ChainPageService) {
  }
  ngOnInit() {
    this.chainPageService.collapseAll.subscribe(value => this.collapseAll = value);
    this.chainPageService.createChainCollapseArray(this.parsers.length);
    this.chainPageService.getCollapseExpandState().subscribe(stateArray => {
      this.parserCollapseStateArray = stateArray;
    });
  }
  removeParser(id: string) {
    this.removeParserEmitter.emit(id);
  }

  onChainSelected(chainId: string) {
    this.chainLevelChange.emit(chainId);
  }

  onParserChange(parser: PartialParserModel) {
    this.parserChange.emit(parser);
  }

  onParserRemove(parserId: string) {
    this.removeParserEmitter.emit(parserId);
  }

  trackByFn(index: number, parserId: string): string {
    return parserId;
  }

  collapseExpandAllParsers() {
    this.chainPageService.collapseExpandAllParsers();
  }

}
