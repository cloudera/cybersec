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

import {ChainModel, ChainOperationalModel, PipelineModel} from '../chain-list-page/chain.model';
import {getHttpParams} from "../chain-list-page/chain-list-page.utils";

@Injectable({
    providedIn: 'root'
})
export class ChainListPageService {

    private readonly BASE_URL = '/api/v1/parserconfig/';

    constructor(
      private http: HttpClient
    ) {}

    public createChain(chain: ChainOperationalModel, pipeline: PipelineModel = null) {
        let httpParams: HttpParams = getHttpParams(pipeline);

        return this.http.post<ChainModel>(this.BASE_URL + 'chains', chain,{params: httpParams});
    }

    public getChains(pipeline: PipelineModel = null, params = null) {
        let httpParams: HttpParams = getHttpParams(pipeline);

        return this.http.get<ChainModel[]>(this.BASE_URL + 'chains',{params: httpParams});
    }

    public deleteChain(chainId: string, pipeline: PipelineModel = null) {
        let httpParams: HttpParams = getHttpParams(pipeline);

        return this.http.delete(this.BASE_URL + 'chains/' + chainId,{params: httpParams});
    }

    public getPipelines() {
        let httpParams: HttpParams = getHttpParams(null);

        return this.http.delete(this.BASE_URL + 'pipeline', {params: httpParams});
    }

}
