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

import {ClusterModel, PipelineModel} from '../chain-list-page/chain.model';
import {map, take} from "rxjs/operators";
import {Observable} from "rxjs";
import {select, Store} from "@ngrx/store";
import {ChainListPageState, getCurrentPipeline} from "../chain-list-page/chain-list-page.reducers";
import {getFinalBaseUrl, getHttpParams} from "../chain-list-page/chain-list-page.utils";

@Injectable({
    providedIn: 'root'
})
export class PipelineService {

    private readonly BASE_URL = '/api/v1/pipeline';

    currentPipeline$: Observable<PipelineModel>;

    constructor(
        private http: HttpClient,
        private store: Store<ChainListPageState>
    ) {
        this.currentPipeline$ = store.pipe(select(getCurrentPipeline))
    }

    public getPipelines(cluster: ClusterModel) {
        let url = getFinalBaseUrl(this.BASE_URL, cluster);

        let httpParams: HttpParams = getHttpParams(cluster);

        return this.http.get<string[]>(url, {params: httpParams})
            .pipe(map(strArr => strArr.map(str => {
                return {name: str} as PipelineModel
            })));
    }

    public getCurrentPipeline() {
        let currentPipeline: PipelineModel;
        this.currentPipeline$.pipe(take(1))
            .subscribe(value => currentPipeline = value);
        return currentPipeline;
    }

}
