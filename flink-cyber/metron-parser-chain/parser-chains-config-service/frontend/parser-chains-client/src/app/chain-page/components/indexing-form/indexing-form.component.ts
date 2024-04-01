import {Component, EventEmitter, OnDestroy, OnInit, Output} from '@angular/core';
import {ConfigChangedEvent} from "../parser/advanced-editor/advanced-editor.component";
import {UntypedFormBuilder, UntypedFormGroup} from "@angular/forms";
import {select, Store} from "@ngrx/store";
import {ChainPageState, getIndexMappings} from "../../chain-page.reducers";
import {GetIndexMappingsAction} from "../../chain-page.actions";
import {takeUntil} from "rxjs/operators";
import {Observable, Subject} from "rxjs";
import {getObjProp} from "../../../shared/utils";

@Component({
  selector: 'app-indexing-form',
  templateUrl: './indexing-form.component.html',
  styleUrls: ['./indexing-form.component.scss']
})
export class IndexingFormComponent implements OnInit, OnDestroy {
  @Output() fieldSetUpdated = new EventEmitter<Map<string, Map<string, boolean>>>();
  form!: UntypedFormGroup;
  mappingJson: any;
  unSubscribe$ = new Subject<void>();
  private _indexMappings$: Observable<{ path: string; result: object }>;

  constructor(private _fb: UntypedFormBuilder,
              private _store: Store<ChainPageState>) {
    this._indexMappings$ = this._store.pipe(select(getIndexMappings), takeUntil(this.unSubscribe$));
  }

  ngOnInit(): void {
    this.form = this._fb.group({filePath: ''});
    this._store.dispatch(new GetIndexMappingsAction({filePath: this.form.value.filePath}));
    this._indexMappings$.subscribe(indexMappings => {
      this.form.patchValue({filePath: indexMappings.path});
      this.onAdvancedEditorChanged({value: indexMappings.result});
    });
  }

  onAdvancedEditorChanged(e: ConfigChangedEvent) {
    const result = new Map<string, Map<string, boolean>>();
    this.mappingJson = e.value;
    Object.keys(this.mappingJson).forEach(key => {
      const sourceMap = new Map<string, boolean>();
      result.set(key, sourceMap);
      const prop = getObjProp(this.mappingJson, key);
      this.findValues(prop, 'ignore_fields').forEach(ignoreList =>
        ignoreList.forEach(value => sourceMap.set(value, true)));

      this.findValues(prop, 'column_mapping').forEach(mapping =>
        this.findValues(mapping, 'name').forEach(value => sourceMap.set(value, false)));
    });
    this.fieldSetUpdated.emit(result);
  }

  findValues(obj, key) {
    let values = [];

    for (const prop in obj) {
      if (prop === key) {
        values.push(obj[prop]);
      } else if (typeof obj[prop] === 'object') {
        values = values.concat(this.findValues(obj[prop], key));
      }
    }
    return values;
  }

  submitForm() {
    this._store.dispatch(new GetIndexMappingsAction(this.form.value));
  }

  ngOnDestroy() {
    this.unSubscribe$.next();
    this.unSubscribe$.complete();
  }
}
