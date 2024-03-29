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

import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';

import {ChainDetailsModel} from '../chain-page/chain-page.models';
import {ParserDescriptor} from "../chain-page/chain-page.reducers";

@Injectable({
    providedIn: 'root'
})
export class ChainPageService {
  static readonly BASE_URL = '/api/v1/parserconfig/';
  public collapseAll = new BehaviorSubject(false);
    private _parserChainCollapseState: BehaviorSubject<boolean[]>;
    private _parserChainSize: number;


    constructor(
      private _http: HttpClient
    ) {}

    public getChain(id: string) {
      return this._http.get(ChainPageService.BASE_URL + `chains/${id}`);
    }

    public getParsers(id: string) {
      return this._http.get(ChainPageService.BASE_URL + `chains/${id}/parsers`);
    }

    public saveParserConfig(chainId: string, config: ChainDetailsModel) {
      return this._http.put(ChainPageService.BASE_URL + `chains/${chainId}`, config);
    }

    public getFormConfig(type: string) {
      return this._http.get<ParserDescriptor>(ChainPageService.BASE_URL + `parser-form-configuration/${type}`);
    }

    public getFormConfigs() {
      return this._http.get<{[key: string] : ParserDescriptor}>(ChainPageService.BASE_URL + `parser-form-configuration`);
    }

    public getIndexMappings(payload?: { filePath: string} ) {
      const finalPayload = payload ? payload : {}
      return this._http.post(ChainPageService.BASE_URL + `indexing`, finalPayload);
    }

    public createChainCollapseArray(size: number) {
      this._parserChainSize = size;
      this._parserChainCollapseState = new BehaviorSubject(new Array(this._parserChainSize).fill(false));
    }
    public getCollapseExpandState() {
      return this._parserChainCollapseState;
    }
    public collapseExpandAllParsers() {
      this.collapseAll.next(!this.collapseAll.value);
      this._parserChainCollapseState.next(new Array(this._parserChainSize).fill(this.collapseAll.value));
    }
}
