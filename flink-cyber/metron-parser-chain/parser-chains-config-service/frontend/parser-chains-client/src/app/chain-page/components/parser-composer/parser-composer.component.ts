import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { select, Store } from '@ngrx/store';

import uuidv1 from 'uuid/v1';

import * as fromActions from '../../chain-page.actions';
import { ParserModel, PartialParserModel } from '../../chain-page.models';
import { ChainPageState, getFormConfigByType, getParser, getParserToBeInvestigated } from '../../chain-page.reducers';
import { CustomFormConfig } from '../custom-form/custom-form.component';

@Component({
  selector: 'app-parser-composer',
  templateUrl: './parser-composer.component.html',
  styleUrls: ['./parser-composer.component.scss']
})
export class ParserComposerComponent implements OnInit {

  @Input() collapsed: boolean;
  @Input() dirty = false;
  @Input() parserId: string;
  @Input() chainId: string;
  @Input() failedParser: string;
  @Output() subchainSelect = new EventEmitter<string>();
  @Output() parserRemove = new EventEmitter<string>();
  @Output() parserChange = new EventEmitter<PartialParserModel>();

  configForm: CustomFormConfig[];
  parserType: string;
  parser: ParserModel;
  isolatedParserView = false;

  constructor(
    private store: Store<ChainPageState>
  ) { }

  ngOnInit() {
    this.store.pipe(select(getParser, {
      id: this.parserId
    })).subscribe((parser) => {
      this.parser = parser;
      if (parser) {
        this.store.pipe(select(getFormConfigByType, { type: parser.type }))
        .subscribe((formConfig) => {
          if (formConfig) {
            this.configForm = formConfig.schemaItems;
            this.parserType = formConfig.name;
          }
        });
      }
    });

    this.store
      .pipe(select(getParserToBeInvestigated))
      .subscribe(id => this.isolatedParserView = !!id);
  }

  onSubchainSelect(chainId: string) {
    this.subchainSelect.emit(chainId);
  }

  onParserChange(partialParser) {
    this.store.dispatch(
      new fromActions.UpdateParserAction({
        chainId: this.chainId,
        parser: partialParser
      })
    );
    this.parserChange.emit(partialParser);
  }

  onRemoveParser(parserId: string) {
    this.parserRemove.emit(parserId);
  }

  onRouteAdd(parser: ParserModel) {
    const chainId = uuidv1();
    const routeId = uuidv1();
    this.store.dispatch(
      new fromActions.AddChainAction({
        chain: {
          id: chainId,
          name: chainId
        }
      })
    );
    this.store.dispatch(
      new fromActions.AddRouteAction({
        chainId,
        parserId: parser.id,
        route: {
          id: routeId,
          name: routeId,
          subchain: chainId,
          default: false,
        }
      })
    );
  }

}
