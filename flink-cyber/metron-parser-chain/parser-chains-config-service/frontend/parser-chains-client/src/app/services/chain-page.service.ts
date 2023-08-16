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

import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';

import {ChainDetailsModel} from '../chain-page/chain-page.models';
import {PipelineModel} from "../chain-list-page/chain.model";
import {getHttpParams} from "../chain-list-page/chain-list-page.utils";

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

    public getChain(id: string, pipeline: PipelineModel = null) {
      let httpParams: HttpParams = getHttpParams(pipeline);

      return this.http.get(this.BASE_URL + `chains/${id}`,{params: httpParams});
    }

    public getParsers(id: string, pipeline: PipelineModel = null) {
      let httpParams: HttpParams = getHttpParams(pipeline);

      return this.http.get(this.BASE_URL + `chains/${id}/parsers`,{params: httpParams});
    }

    public saveParserConfig(chainId: string, config: ChainDetailsModel, pipeline: PipelineModel = null) {
      let httpParams: HttpParams = getHttpParams(pipeline);

      return this.http.put(this.BASE_URL + `chains/${chainId}`, config,{params: httpParams});
    }

    public getFormConfig(type: string) {
      let httpParams: HttpParams = getHttpParams(null);

      return this.http.get(this.BASE_URL + `parser-form-configuration/${type}`,{params: httpParams});
    }

    public getFormConfigs() {
      let httpParams: HttpParams = getHttpParams(null);

      return this.http.get(this.BASE_URL + `parser-form-configuration`,{params: httpParams});
    }

    public getIndexMappings(payload?: { filePath: string }) {
      let httpParams: HttpParams = getHttpParams(null);

      let finalPayload = payload ? payload : {}
      return this.http.post(this.BASE_URL + `indexing`, finalPayload,{params: httpParams});
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
