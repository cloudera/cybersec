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

export interface ParserChainModel {
  id: string;
  name: string;
  parsers: string[] | ParserModel[];
}

export interface PartialParserChainModel {
  id?: string;
  name?: string;
  parsers?: string[] | ParserModel[];
}

export interface RouteModel {
  id: string;
  name: string;
  default: boolean;
  subchain: string | ParserChainModel;
  matchingValue?: string;
}

export interface PartialRouteModel {
  id?: string;
  name?: string;
  default?: boolean;
  subchain?: string | ParserChainModel;
  matchingValue?: string;
}

export interface ParserModel {
  id: string;
  type: string;
  name: string;
  config?: any;
  routing?: any;
}

export interface PartialParserModel {
  id?: string;
  type?: string;
  name?: string;
  config?: any;
  routing?: any;
}

export interface ChainDetailsModel {
  id: string;
  name: string;
  parsers: ParserModel[];
}

export interface IndexTableMapping {
  table_name: string;
  ignore_fields?: string[]
  column_mapping: IndexingColumnMapping[]
}

export interface IndexingColumnMapping {
  name: string;
  kafka_name?: string;
  path?: string;
  transformation?: string;
  is_map?: boolean;
}
