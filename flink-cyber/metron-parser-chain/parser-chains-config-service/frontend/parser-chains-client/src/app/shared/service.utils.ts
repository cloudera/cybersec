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

import {HttpParams} from "@angular/common/http";

export function getHttpParams(map: string | Map<string,string> | {[key:string] : string}): HttpParams;

export function getHttpParams(arg: any) {
  if (arg === null || arg === undefined) {
    return new HttpParams();
  }
  if (typeof arg === 'string' && arg) {
    return new HttpParams().set('pipelineName', arg);
  }
  if (arg instanceof Map) {
    return arg;
  }
  if (typeof arg === 'object') {
    return new HttpParams({fromObject: arg});
  }
  return new HttpParams();
}
