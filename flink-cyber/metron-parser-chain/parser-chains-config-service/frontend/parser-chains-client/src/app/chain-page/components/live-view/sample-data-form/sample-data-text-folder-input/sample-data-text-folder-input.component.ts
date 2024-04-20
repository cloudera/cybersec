import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
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
    getSampleData,
    getSampleFolderPath
} from "./sample-data-text-folder-input.selectors";
import {Observable, Subject} from "rxjs";
import {
    FetchSampleListTriggeredAction,
    SampleFolderViewInitializedAction,
    SaveSampleListTriggeredAction,
    ShowEditModalAction
} from "./sample-data-text-folder-input.actions";
import {UntypedFormBuilder, UntypedFormGroup} from "@angular/forms";
import {map, takeUntil} from "rxjs/operators";

@Component({
  selector: 'app-sample-data-text-folder-input',
  templateUrl: './sample-data-text-folder-input.component.html',
  styleUrls: ['./sample-data-text-folder-input.component.scss']
})
export class SampleDataTextFolderInputComponent implements OnInit, OnDestroy {
  @Input() chainConfig: any;
  @Output() sampleDataChange = new EventEmitter<SampleDataModel>();
  @Output() sampleDataForceChange = new EventEmitter<SampleDataInternalModel[]>();

  isExecuting$: Observable<boolean>;
  runResults$: Observable<Map<number, {
    status: SampleTestStatus,
    expected: string,
    result: string,
    failure: boolean,
    raw: EntryParsingResultModel[],
    timestamp: bigint
  }>>;
  sampleData$: Observable<SampleDataInternalModel[]>;
  sampleFolderPath$: Observable<string>;
  editSampleModalVisible$: Observable<boolean>;
  expandSet = new Set<number>();
  folderForm!: UntypedFormGroup;
  currentSampleData: SampleDataInternalModel[];
  selectedSample: [number, SampleDataInternalModel] = [null, null];
  protected readonly sampleTestStatus = SampleTestStatus;
  private _unsubscribe$: Subject<void> = new Subject<void>();


  constructor(private _store: Store<SampleDataTextFolderInputState>,
              private _fb: UntypedFormBuilder) {
    this.isExecuting$ = this._store.pipe(select(getExecutionStatus));
    this.runResults$ = this._store.pipe(select(getRunResults));
    this.sampleData$ = this._store.pipe(select(getSampleData));
    this.sampleFolderPath$ = this._store.pipe(select(getSampleFolderPath));
    this.editSampleModalVisible$ = _store.pipe(select(getEditModalVisible));
    this.sampleData$.pipe(takeUntil(this._unsubscribe$)).subscribe(value => this.currentSampleData = value)
  }

  get folderPath() {
    return this.folderForm.get("folderPath")
  }

  ngOnInit(): void {
    this._store.dispatch(SampleFolderViewInitializedAction());
    this.folderForm = this._fb.group({folderPath: ""});
    this.sampleFolderPath$.pipe(takeUntil(this._unsubscribe$)).subscribe(value => {
      this.folderForm.patchValue({folderPath: value});
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
    this._store.dispatch(FetchSampleListTriggeredAction({
      folderPath: this.folderPath.value,
      chainId: this.chainConfig.id
    }))
  }

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
    this._store.dispatch(ShowEditModalAction())
  }

  updateSample($event: SampleDataInternalModel) {
    const newSampleList = this.currentSampleData
      ? [...this.currentSampleData]
      : []

    if (this.selectedSample[0] == null) {
      //new sample
      newSampleList.push($event)
    } else {
      //existing sample
      newSampleList[this.selectedSample[0]] = $event
    }

    this._store.dispatch(SaveSampleListTriggeredAction({
      folderPath: this.folderPath.value,
      chainId: this.chainConfig.id,
      sampleList: newSampleList
    }))
  }

  deleteSample($event: MouseEvent, data: SampleDataInternalModel, i: number) {
    if (!this.currentSampleData) {
      return
    }
    const newSampleList = [...this.currentSampleData]
    newSampleList.splice(i, 1);

    this._store.dispatch(SaveSampleListTriggeredAction({
      folderPath: this.folderPath.value,
      chainId: this.chainConfig.id,
      sampleList: newSampleList
    }))

  }

  addSample() {
    const maxId: number = this.currentSampleData
      ? Math.max(...this.currentSampleData.map(v => v.id))
      : 0;
    this.selectedSample = [null, {
      id: maxId + 1,
      source: "",
      name: "",
      description: "",
      expectedFailure: false,
      expectedResult: ""
    }]
    this._store.dispatch(ShowEditModalAction())
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

  getStatus(id: number) {
    return this.runResults$.pipe(map(value => {
      const dataById = value.get(id);
      return dataById === undefined
        ? SampleTestStatus.UNKNOWN
        : dataById.status;
    }));
  }

  updateExpectedValue(failure: boolean, result: string, i: number, timestamp: bigint) {
    const sample = this.currentSampleData[i];

    this.selectedSample = [i, sample]

    let finalResult = result

    if (timestamp){
      finalResult = finalResult.replace(String(timestamp), "%timestamp%")
    }

    this.updateSample({
      ...sample,
      expectedFailure: failure,
      expectedResult: finalResult
    })
  }

  ngOnDestroy(): void {
    this._unsubscribe$.next();
    this._unsubscribe$.complete();
  }
}
