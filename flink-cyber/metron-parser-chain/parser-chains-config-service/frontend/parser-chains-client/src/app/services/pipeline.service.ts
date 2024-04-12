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

import {map, take} from "rxjs/operators";
import {Observable} from "rxjs";
import {select, Store} from "@ngrx/store";
import {ChainListPageState, getSelectedPipeline} from "../chain-list-page/chain-list-page.reducers";
import {getHttpParams} from "../chain-list-page/chain-list-page.utils";

@Injectable({
    providedIn: 'root'
})
export class PipelineService {

    private readonly BASE_URL = '/api/v1/pipeline';

    currentPipeline$: Observable<string>;

    constructor(
        private http: HttpClient,
        private store: Store<ChainListPageState>
    ) {
        this.currentPipeline$ = store.pipe(select(getSelectedPipeline))
    }

    public getPipelines() {
        let httpParams: HttpParams = getHttpParams(null);

        return this.http.get<string[]>(this.BASE_URL, {params: httpParams})
            .pipe(map(strArr => strArr.map(pipelineName => {
                return pipelineName
            })));
    }

    public createPipeline(pipelineName: string) {
        let httpParams: HttpParams = getHttpParams(null);

        return this.http.post<string[]>(this.BASE_URL + "/" + pipelineName,
            null,{params: httpParams})
            .pipe(map(strArr => strArr.map(pipelineName => {
                return pipelineName
            })));
    }

    public renamePipeline(pipelineName: string, newPipelineName: string) {
        let httpParams: HttpParams = getHttpParams(null);
        httpParams = httpParams.set('newName', newPipelineName)

        return this.http.put<string[]>(this.BASE_URL + "/" + pipelineName,
            null,{params: httpParams})
            .pipe(map(strArr => strArr.map(pipelineName => {
                return pipelineName
            })));
    }

    public deletePipeline(pipelineName: string) {
        let httpParams: HttpParams = getHttpParams(null);

        return this.http.delete<string[]>(this.BASE_URL + "/" + pipelineName, {params: httpParams})
            .pipe(map(strArr => strArr.map(pipelineName => {
                return pipelineName
            })));
    }

    public getCurrentPipeline() {
        let currentPipeline: string;
        this.currentPipeline$.pipe(take(1))
            .subscribe(pipelineName => currentPipeline = pipelineName);
        return currentPipeline;
    }

}
