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

import { SampleDataRequestModel } from './sample-data.model';

export interface EntryParsingResultModel {
  output: {};
  log: {
    type: string;
    message: string;
    parserId?: string;
    stackTrace: string;
  };
  parserResults?: ParserResultsModel[];
}

export interface ParserResultsModel {
  output: {};
  log: {
    type: string;
    message: string;
    parserId?: string;
    parserName?: string;
    stackTrace: string;
  };
}

export interface LiveViewRequestModel {
  sampleData: SampleDataRequestModel;
  chainConfig: {};
}