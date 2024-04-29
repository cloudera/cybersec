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

import {SampleDataInternalModel, SampleTestStatus} from "../../models/sample-data.model";
import {
    ExecutionListFailedAction,
    ExecutionListSuccessfulAction,
    ExecutionListTriggeredAction,
    FetchSampleListFailedAction,
    FetchSampleListSuccessfulAction,
    FetchSampleListTriggeredAction,
    HideEditModalAction,
    SampleFolderActionsType,
    SampleFolderPathRestoredAction,
    SaveSampleListFailedAction,
    SaveSampleListSuccessfulAction,
    SaveSampleListTriggeredAction,
    ShowEditModalAction
} from "./sample-data-text-folder-input.actions";
import {EntryParsingResultModel} from "../../models/live-view.model";

export interface SampleDataTextFolderInputState {
    isTestExecuting: boolean;
    isFetchExecuting: boolean;
    isSaveExecuting: boolean;
    editModalShown: boolean;
    chainId: string;
    sampleData: SampleDataInternalModel[];
    sampleFolderPath: string,
    runResult: Map<number, {
        status: SampleTestStatus,
        expected: string,
        result: string,
        failure: boolean,
        raw: EntryParsingResultModel[],
        timestamp: bigint
    }>;
}

export const initialState: SampleDataTextFolderInputState = {
    isTestExecuting: false,
    isFetchExecuting: false,
    isSaveExecuting: false,
    chainId: "",
    editModalShown: false,
    sampleData: [],
    sampleFolderPath: "",
    runResult: new Map()
};

export function reducer(
    state: SampleDataTextFolderInputState = initialState,
    action: SampleFolderActionsType
): SampleDataTextFolderInputState {
    switch (action.type) {
        case SampleFolderPathRestoredAction.type: {
            if (action.sampleFolderPath === null) { return state; }

            return {
                ...state,
                sampleFolderPath: action.sampleFolderPath
            }
        }
        case ExecutionListTriggeredAction.type: {
            return {
                ...state,
                sampleData: action.sampleData,
                chainId: action.chainConfig.id,
                isTestExecuting: true,
            };
        }
        case ExecutionListSuccessfulAction.type: {
            return {
                ...state,
                isTestExecuting: false,
                runResult: prepareResult(action.sampleFolderResults),
            };
        }
        case ExecutionListFailedAction.type: {
            return {
                ...state,
                isTestExecuting: false,
            };
        }
        case ShowEditModalAction.type: {
            return {
                ...state,
                editModalShown: true,
            };
        }
        case HideEditModalAction.type: {
            return {
                ...state,
                editModalShown: false,
            };
        }
        case SaveSampleListTriggeredAction.type: {
            return {
                ...state,
                isSaveExecuting: true,
            };
        }
        case SaveSampleListSuccessfulAction.type: {
            return {
                ...state,
                sampleData: action.saveResults,
                isSaveExecuting: false,
            };
        }
        case SaveSampleListFailedAction.type: {
            return {
                ...state,
                isSaveExecuting: false,
            };
        }
        case FetchSampleListTriggeredAction.type: {
            return {
                ...state,
                runResult: new Map(),
                isFetchExecuting: true,
                sampleFolderPath: action.folderPath
            };
        }
        case FetchSampleListSuccessfulAction.type: {
            return {
                ...state,
                sampleData: action.fetchResult,
                isFetchExecuting: false
            };
        }
        case FetchSampleListFailedAction.type: {
            return {
                ...state,
                isFetchExecuting: false
            };
        }
        default: {
            return state;
        }
    }
}

function prepareResult(rawResult: Map<number, [SampleDataInternalModel, EntryParsingResultModel[]]>): Map<number, {
    status: SampleTestStatus,
    expected: string,
    result: string,
    failure: boolean,
    raw: EntryParsingResultModel[],
    timestamp: bigint
}> {
    const resultMap = new Map<number, {
        status: SampleTestStatus,
        expected: string,
        result: string,
        failure: boolean,
        raw: EntryParsingResultModel[],
        timestamp: bigint
    }>();
    rawResult.forEach((value, key) => {
        const sample = value[0];
        const result = value[1];

        const failedParser = result.find((res) => res.log.type === 'error');

        let status: SampleTestStatus;
        let output: string;
        let failure: boolean;
        let finalExpectedResult = sample.expectedResult;
        let timestamp = null;

        if (failedParser) {
            output = failedParser.log.message;
            failure = true;

            if (sample.expectedFailure && output === sample.expectedResult) {
                status = SampleTestStatus.SUCCESS
            } else {
                status = SampleTestStatus.FAIL
            }
        } else {
            const rawOutput = result[result.length - 1].output as { timestamp: string };
            timestamp = rawOutput.timestamp;

            output = JSON.stringify(rawOutput);
            failure = false;

            finalExpectedResult = finalExpectedResult.replace('%timestamp%', timestamp);

            if (!sample.expectedFailure && output === finalExpectedResult) {
                status = SampleTestStatus.SUCCESS
            } else {
                status = SampleTestStatus.FAIL
            }
        }

        resultMap.set(key, {
            status,
            expected: finalExpectedResult,
            result: output,
            failure,
            raw: result,
            timestamp: timestamp
        })
    })
    return resultMap
}

