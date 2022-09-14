import { SampleDataRequestModel } from './sample-data.model';

export interface EntryParsingResultModel {
  output: {};
  log: {
    type: string;
    message: string;
    parserId?: string;
    stackTrace: string;
  };
  parserResults?: ParserResultsModel;
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
