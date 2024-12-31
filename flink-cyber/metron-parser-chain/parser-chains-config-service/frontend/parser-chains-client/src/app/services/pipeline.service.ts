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

import {map} from 'rxjs/operators';
import {getHttpParams} from '../shared/service.utils';

@Injectable({
    providedIn: 'root'
})
export class PipelineService {

    static readonly BASE_URL = '/api/v1/pipeline';

    constructor(
        private _http: HttpClient,
    ) {
    }

    public getPipelines() {
        const httpParams: HttpParams = getHttpParams(null);
        return this._http.get<string[]>(PipelineService.BASE_URL, {params: httpParams})
            .pipe(map(strArr => strArr.map(pipelineName => {
                return pipelineName
            })));
    }

    public createPipeline(pipelineName: string) {
        const httpParams: HttpParams = getHttpParams(null);

        return this._http.post<string[]>(PipelineService.BASE_URL + "/" + pipelineName,
            null,{params: httpParams})
            .pipe(map(strArr => strArr.map(pName => {
                return pName
            })));
    }

    public renamePipeline(pipelineName: string, newPipelineName: string) {
        let httpParams: HttpParams = getHttpParams(null);
        httpParams = httpParams.set('newName', newPipelineName)

        return this._http.put<string[]>(PipelineService.BASE_URL + "/" + pipelineName,
            null,{params: httpParams})
            .pipe(map(strArr => strArr.map(pName => {
                return pName
            })));
    }

    public deletePipeline(pipelineName: string) {
        const httpParams: HttpParams = getHttpParams(null);

        return this._http.delete<string[]>(PipelineService.BASE_URL + "/" + pipelineName, {params: httpParams})
            .pipe(map(strArr => strArr.map(pName => {
                return pName
            })));
    }
}
