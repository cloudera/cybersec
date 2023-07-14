import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {Store} from "@ngrx/store";
import {SampleDataTextFolderInputState} from "../sample-data-text-folder-input.reducers";
import {HideEditModalAction} from "../sample-data-text-folder-input.actions";
import {SampleDataInternalModel} from "../../../models/sample-data.model";

@Component({
    selector: 'app-sample-edit-popup',
    templateUrl: './sample-edit-popup.component.html',
    styleUrls: ['./sample-edit-popup.component.scss']
})
export class SampleEditPopupComponent implements OnChanges {

    @Input() modalVisible: boolean;
    @Input() sample: SampleDataInternalModel;

    @Output() sampleDataChange = new EventEmitter<SampleDataInternalModel>();

    editSampleForm: FormGroup;
    isOkLoading = false;


    constructor(private store: Store<SampleDataTextFolderInputState>,
                private fb: FormBuilder) {

    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.sample !== undefined && !changes.sample.isFirstChange()){
            this.editSampleForm = this.fb.group({
                source: new FormControl(this.sample.source),
                name: new FormControl(this.sample.name, [Validators.required, Validators.minLength(3)]),
                description: new FormControl(this.sample.description),
                expectedFailure: new FormControl(this.sample.expectedFailure, Validators.required),
                expectedResult: new FormControl(this.sample.expectedResult)
            });
        }
    }

    get source() {
        return this.editSampleForm.get('source')
    }

    get name() {
        return this.editSampleForm.get('name')
    }

    get description() {
        return this.editSampleForm.get('description')
    }

    get expectedFailure() {
        return this.editSampleForm.get('expectedFailure')
    }

    get expectedResult() {
        return this.editSampleForm.get('expectedResult')
    }

    pushChain() {
        this.sampleDataChange.emit({
            ...this.sample,
            source: this.source.value,
            name: this.name.value,
            description: this.description.value,
            expectedFailure: this.expectedFailure.value,
            expectedResult: this.expectedResult.value
        })
        this.store.dispatch(HideEditModalAction())
    }

    handleCancelChainModal() {
        this.store.dispatch(HideEditModalAction())
    }
}
