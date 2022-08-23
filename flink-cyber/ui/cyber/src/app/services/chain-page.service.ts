import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { ChainDetailsModel } from '../parsers/parsers.models';

@Injectable({
    providedIn: 'root'
})
export class ChainPageService {
    private parserChainCollapseState: BehaviorSubject<boolean[]>;
    private parserChainSize: number;
    private readonly BASE_URL = '/api/v1/parserconfig/';
    public collapseAll = new BehaviorSubject(false);

    constructor(
      private http: HttpClient
    ) {}

    public getChain(id: string) {
      return this.http.get(this.BASE_URL + `chains/${id}`);
    }

    public getParsers(id: string) {
      return this.http.get(this.BASE_URL + `chains/${id}/parsers`);
    }

    public saveParserConfig(chainId: string, config: ChainDetailsModel) {
      return this.http.put(this.BASE_URL + `chains/${chainId}`, config);
    }

    public getFormConfig(type: string) {
      return this.http.get(this.BASE_URL + `parser-form-configuration/${type}`);
    }

    public getFormConfigs() {
      return this.http.get(this.BASE_URL + `parser-form-configuration`);
    }

    public createChainCollapseArray(size: number) {
      this.parserChainSize = size;
      this.parserChainCollapseState = new BehaviorSubject(new Array(this.parserChainSize).fill(false));
    }
    public getCollapseExpandState() {
      return this.parserChainCollapseState;
    }
    public collapseExpandAllParsers() {
      this.collapseAll.next(!this.collapseAll.value);
      this.parserChainCollapseState.next(new Array(this.parserChainSize).fill(this.collapseAll.value));
    }
}
