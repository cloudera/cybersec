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
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { ChainDetailsModel } from '../chain-page/chain-page.models';
import {ClusterModel, PipelineModel} from "../chain-list-page/chain.model";
import {
    getFinalBaseUrl,
    getHttpParams
} from "../chain-list-page/chain-list-page.utils";

@Injectable({
    providedIn: 'root'
})
export class ChainPageService {
    private parserChainCollapseState: BehaviorSubject<boolean[]>;
    private parserChainSize: number;
    private readonly URL_PREFIX = '/api/v1';
    private readonly BASE_URL = '/parserconfig/';
    public collapseAll = new BehaviorSubject(false);

    constructor(
      private http: HttpClient
    ) {}

    public getChain(id: string, pipeline: PipelineModel = null, cluster: ClusterModel) {
      let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

      let httpParams: HttpParams = getHttpParams(pipeline, cluster);

      return this.http.get(url + `chains/${id}`,{params: httpParams});
    }

    public getParsers(id: string, pipeline: PipelineModel = null, cluster: ClusterModel) {
      let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

      let httpParams: HttpParams = getHttpParams(pipeline, cluster);

      return this.http.get(url + `chains/${id}/parsers`,{params: httpParams});
    }

    public saveParserConfig(chainId: string, config: ChainDetailsModel, pipeline: PipelineModel = null, cluster: ClusterModel) {
      let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

      let httpParams: HttpParams = getHttpParams(pipeline, cluster);

      return this.http.put(url + `chains/${chainId}`, config,{params: httpParams});
    }

    public getFormConfig(type: string, cluster: ClusterModel) {
      let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

      let httpParams: HttpParams = getHttpParams(null, cluster);

      return this.http.get(url + `parser-form-configuration/${type}`,{params: httpParams});
    }

    public getFormConfigs(cluster: ClusterModel) {
      let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

      let httpParams: HttpParams = getHttpParams(null, cluster);

      return this.http.get(url + `parser-form-configuration`,{params: httpParams});
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
