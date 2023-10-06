import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {ConfigChangedEvent} from "../parser/advanced-editor/advanced-editor.component";
import {ChainPageService} from "../../../services/chain-page.service";
import {FormBuilder, FormGroup} from "@angular/forms";
import {select, Store} from "@ngrx/store";
import {ChainPageState, getIndexMappings} from "../../chain-page.reducers";
import {GetIndexMappingsAction} from "../../chain-page.actions";

@Component({
    selector: 'app-indexing-form',
    templateUrl: './indexing-form.component.html',
    styleUrls: ['./indexing-form.component.scss']
})
export class IndexingFormComponent implements OnInit {

    private readonly _indexingPathKey = 'indexingPath';

    form!: FormGroup;
    @Output() fieldSetUpdated = new EventEmitter<Map<string, Map<string, boolean>>>()
    mappingJson: any;

    constructor(public chainPageService: ChainPageService,
                private fb: FormBuilder,
                private store: Store<ChainPageState>) {
    }

    ngOnInit(): void {
        this.form = this.fb.group({filePath: ''})
        this.form.setValue(JSON.parse(localStorage.getItem(this._indexingPathKey)))
        this.submitForm()
    }

    onAdvancedEditorChanged(e: ConfigChangedEvent) {
        let result = new Map<string, Map<string, boolean>>()

        this.mappingJson = e['value']
        for (let s in this.mappingJson) {
            let sourceMap = new Map<string, boolean>();
            result.set(s, sourceMap)
            this.findValues(this.mappingJson[s], 'ignore_fields').forEach(ignore_list =>
                ignore_list.forEach(value => sourceMap.set(value, true)));

            this.findValues(this.mappingJson[s], 'column_mapping').forEach(mapping =>
                this.findValues(mapping, 'name').forEach(value => sourceMap.set(value, false)))
        }
        this.fieldSetUpdated.emit(result)
    }

    findValues(obj, key) {
        let values = [];

        for (let prop in obj) {
            if (prop === key) {
                values.push(obj[prop]);
            } else if (typeof obj[prop] === 'object') {
                values = values.concat(this.findValues(obj[prop], key));
            }
        }

        return values;
    }

    submitForm() {
        localStorage.setItem(this._indexingPathKey, JSON.stringify(this.form.value))
        this.store.dispatch(new GetIndexMappingsAction(this.form.value))
        this.store.pipe(select(getIndexMappings))
            .subscribe(indexMappings => {
                this.form.setValue({filePath: indexMappings.path});
                this.onAdvancedEditorChanged({value: indexMappings.result})
            })
    }
}
