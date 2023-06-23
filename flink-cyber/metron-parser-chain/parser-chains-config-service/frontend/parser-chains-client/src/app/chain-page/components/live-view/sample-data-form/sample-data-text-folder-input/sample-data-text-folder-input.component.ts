import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {
    SampleDataInternalModel,
    SampleDataModel,
    SampleDataType,
    SampleTestStatus
} from "../../models/sample-data.model";
import {EntryParsingResultModel} from "../../models/live-view.model";
import {select, Store} from "@ngrx/store";
import {SampleDataTextFolderInputState} from "./sample-data-text-folder-input.reducers";
import {
    getEditModalVisible,
    getExecutionStatus,
    getRunResults,
    getSampleData, getSampleFolderPath
} from "./sample-data-text-folder-input.selectors";
import {Observable} from "rxjs";
import {
    FetchSampleListTriggeredAction, SampleFolderViewInitializedAction,
    SaveSampleListTriggeredAction,
    ShowEditModalAction
} from "./sample-data-text-folder-input.actions";
import {FormBuilder, FormGroup} from "@angular/forms";
import {liveViewInitialized} from "../../live-view.actions";
import {map} from "rxjs/operators";

@Component({
    selector: 'app-sample-data-text-folder-input',
    templateUrl: './sample-data-text-folder-input.component.html',
    styleUrls: ['./sample-data-text-folder-input.component.scss']
})
export class SampleDataTextFolderInputComponent implements OnInit {

    @Input() chainConfig: {};
    @Output() sampleDataChange = new EventEmitter<SampleDataModel>();
    @Output() sampleDataForceChange = new EventEmitter<SampleDataInternalModel[]>();

    isExecuting$: Observable<boolean>;
    runResults$: Observable<Map<number, {
        status: SampleTestStatus,
        expected: string,
        result: string,
        failure: boolean,
        raw: EntryParsingResultModel[]
    }>>;
    sampleData$: Observable<SampleDataInternalModel[]>;
    sampleFolderPath$: Observable<string>;

    expandSet = new Set<number>();

    folderForm!: FormGroup;
    currentSampleData: SampleDataInternalModel[];

    selectedSample: [number, SampleDataInternalModel] = [null, null];

    constructor(private store: Store<SampleDataTextFolderInputState>,
                private fb: FormBuilder) {
        this.isExecuting$ = this.store.pipe(select(getExecutionStatus));
        this.runResults$ = this.store.pipe(select(getRunResults));
        this.sampleData$ = this.store.pipe(select(getSampleData));
        this.sampleFolderPath$ = this.store.pipe(select(getSampleFolderPath));

        this.sampleData$.subscribe(value => this.currentSampleData = value)
    }

    get folderPath() {
        return this.folderForm.get("folderPath")
    }

    ngOnInit(): void {
        this.store.dispatch(SampleFolderViewInitializedAction());
        this.sampleFolderPath$.subscribe(value => {
            this.folderForm = this.fb.group({folderPath: value})
            this.fetchSamples()
        })
    }

    onExpandChange(id: number, checked: boolean): void {
        if (checked) {
            this.expandSet.add(id);
        } else {
            this.expandSet.delete(id);
        }
    }

    fetchSamples() {
        this.store.dispatch(FetchSampleListTriggeredAction({
            folderPath: this.folderPath.value,
            chainId: this.chainConfig['id']
        }))
    }

    protected readonly SampleTestStatus = SampleTestStatus;

    applySample($event: MouseEvent, sample: SampleDataInternalModel) {
        this.sampleDataChange.emit({
            source: sample.source,
            type: SampleDataType.MANUAL
        });
    }

    runSamples() {
        this.sampleDataForceChange.emit(this.currentSampleData)
    }

    editSample($event: MouseEvent, data: SampleDataInternalModel, i: number) {
        this.selectedSample = [i, data]
        this.store.dispatch(ShowEditModalAction())
    }

    updateSample($event: SampleDataInternalModel) {
        let newSampleList = this.currentSampleData
            ? [...this.currentSampleData]
            : []

        if (this.selectedSample[0] == null){
            //new sample
            newSampleList.push($event)
        } else {
            //existing sample
            newSampleList[this.selectedSample[0]] = $event
        }

        this.store.dispatch(SaveSampleListTriggeredAction({
            folderPath: this.folderPath.value,
            chainId: this.chainConfig['id'],
            sampleList: newSampleList
        }))
    }

    deleteSample($event: MouseEvent, data: SampleDataInternalModel, i: number) {
        if (!this.currentSampleData){
            return
        }
        let newSampleList = [...this.currentSampleData]
        newSampleList.splice(i, 1);

        this.store.dispatch(SaveSampleListTriggeredAction({
            folderPath: this.folderPath.value,
            chainId: this.chainConfig['id'],
            sampleList: newSampleList
        }))

    }

    addSample() {
        let maxId: number = this.currentSampleData
            ? Math.max(...this.currentSampleData.map(v => v.id))
            : 0;
        this.selectedSample = [null, {
            id: maxId+1,
            source: "",
            name: "",
            description: "",
            expectedFailure: false,
            expectedResult: ""
        }]
        this.store.dispatch(ShowEditModalAction())
    }

    getIconType(id: number) {
        return this.getStatus(id).pipe(map(dataStatus => {
            switch (dataStatus) {
                case SampleTestStatus.SUCCESS:
                    return "check-circle"
                case SampleTestStatus.FAIL:
                    return "close-circle"
                case SampleTestStatus.UNKNOWN:
                    return "clock-circle"
            }
        }))
    }

    getIconColor(id: number) {
        return this.getStatus(id).pipe(map(dataStatus => {
            switch (dataStatus) {
                case SampleTestStatus.SUCCESS:
                    return "#52c41a"
                case SampleTestStatus.FAIL:
                    return "#d41f1f"
                case SampleTestStatus.UNKNOWN:
                    return "#bbbbbb"
            }
        }))
    }

    getStatus(id: number){
        return this.runResults$.pipe(map(value => {
            let dataById = value.get(id);
            return dataById === undefined
                ? SampleTestStatus.UNKNOWN
                : dataById.status;
        }));
    }
}
