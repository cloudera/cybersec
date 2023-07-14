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

import {ChainModel, ChainOperationalModel, ClusterModel, PipelineModel} from '../chain-list-page/chain.model';
import {
    getFinalBaseUrl,
    getHttpParams
} from "../chain-list-page/chain-list-page.utils";

@Injectable({
    providedIn: 'root'
})
export class ChainListPageService {

    private readonly URL_PREFIX = '/api/v1';
    private readonly BASE_URL = '/parserconfig/';

    constructor(
      private http: HttpClient
    ) {}

    public createChain(chain: ChainOperationalModel, pipeline: PipelineModel = null, cluster: ClusterModel) {
        let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

        let httpParams: HttpParams = getHttpParams(pipeline, cluster);

        return this.http.post<ChainModel>(url + 'chains', chain,{params: httpParams});
    }

    public getChains(pipeline: PipelineModel = null, cluster: ClusterModel, params = null) {
        let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

        let httpParams: HttpParams = getHttpParams(pipeline, cluster);

        return this.http.get<ChainModel[]>(url + 'chains',{params: httpParams});
    }

    public deleteChain(chainId: string, pipeline: PipelineModel = null, cluster: ClusterModel) {
        let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

        let httpParams: HttpParams = getHttpParams(pipeline, cluster);

        return this.http.delete(url + 'chains/' + chainId,{params: httpParams});
    }

    public getPipelines(cluster: ClusterModel) {
        let url = getFinalBaseUrl(this.URL_PREFIX, this.BASE_URL, cluster);

        let httpParams: HttpParams = getHttpParams(null, cluster);

        return this.http.delete(url + 'pipeline', {params: httpParams});
    }

}
