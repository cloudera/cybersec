import {Component, EventEmitter, OnDestroy, OnInit, Output} from '@angular/core';
import {ConfigChangedEvent} from "../parser/advanced-editor/advanced-editor.component";
import {FormBuilder, FormGroup, UntypedFormBuilder, UntypedFormGroup} from "@angular/forms";
import {select, Store} from "@ngrx/store";
import {ChainPageState, getIndexMappings} from "../../chain-page.reducers";
import {GetIndexMappingsAction} from "../../chain-page.actions";
import {takeUntil} from "rxjs/operators";
import {Observable, Subject} from "rxjs";

@Component({
  selector: 'app-indexing-form',
  templateUrl: './indexing-form.component.html',
  styleUrls: ['./indexing-form.component.scss']
})
export class IndexingFormComponent implements OnInit, OnDestroy {

  form!: UntypedFormGroup;
  @Output() fieldSetUpdated = new EventEmitter<Map<string, Map<string, boolean>>>();
  mappingJson: any;
  unSubscribe$ = new Subject<void>();
  private indexMappings$: Observable<{ path: string; result: object }>;

  constructor(private fb: UntypedFormBuilder,
              private store: Store<ChainPageState>) {
    this.indexMappings$ = this.store.pipe(select(getIndexMappings), takeUntil(this.unSubscribe$));
  }

  ngOnInit(): void {
    this.form = this.fb.group({filePath: ''});
    this.indexMappings$.subscribe(indexMappings => {
      this.store.dispatch(new GetIndexMappingsAction({filePath: indexMappings.path}));
      this.form.patchValue({filePath: indexMappings.path});
      this.onAdvancedEditorChanged({value: indexMappings.result});
    });
  }

  onAdvancedEditorChanged(e: ConfigChangedEvent) {
    let result = new Map<string, Map<string, boolean>>();

    this.mappingJson = e['value'];
    for (let s in this.mappingJson) {
      let sourceMap = new Map<string, boolean>();
      result.set(s, sourceMap);
      this.findValues(this.mappingJson[s], 'ignore_fields').forEach(ignore_list =>
        ignore_list.forEach(value => sourceMap.set(value, true)));

      this.findValues(this.mappingJson[s], 'column_mapping').forEach(mapping =>
        this.findValues(mapping, 'name').forEach(value => sourceMap.set(value, false)));
    }
    this.fieldSetUpdated.emit(result);
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
    this.store.dispatch(new GetIndexMappingsAction(this.form.value));
  }

  ngOnDestroy() {
    this.unSubscribe$.next();
    this.unSubscribe$.complete();
  }
}
