import {Component, OnInit} from '@angular/core';
import {BehaviorSubject, Observable} from "rxjs";
import {tap} from "rxjs/operators";
import {OcsfService} from "../../../services/ocsf.service";
import {BaseOcsfSchemaModel, OcsfSchemaModel} from "./ocsf-form.model";
import {UntypedFormBuilder, UntypedFormGroup} from "@angular/forms";
import {EntryParsingResultModel} from "../live-view/models/live-view.model";
import {select, Store} from "@ngrx/store";
import {LiveViewState} from "../live-view/live-view.reducers";
import {getResults} from "../live-view/live-view.selectors";
import {IndexingColumnMapping, IndexTableMapping} from "../../chain-page.models";
import {ChainPageService} from "../../../services/chain-page.service";
import {HttpResponse} from "@angular/common/http";
import {NzMessageService} from "ng-zorro-antd/message";

@Component({
  selector: 'app-ocsf-form',
  templateUrl: './ocsf-form.component.html',
  styleUrls: ['./ocsf-form.component.scss']
})
export class OcsfFormComponent implements OnInit {

  schema$: Observable<OcsfSchemaModel> = this._ocsfService.getSchema().pipe(tap(schema => {
    const profileList = Array.from(
      new Set(
        Object.values(schema.classes).flatMap(({profiles}) => profiles)
      )
    );
    this.profileList$.next(profileList)
    this.isLoading$.next(false)
  }));
  isLoading$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(true);
  profileList$: BehaviorSubject<string[]> = new BehaviorSubject<string[]>([]);
  filtersForm: UntypedFormGroup;
  ocsfForm: UntypedFormGroup;
  results$: Observable<EntryParsingResultModel[]>;
  selectedClassFields: string[] = [];
  selectedClassFilters: string[] = [];
  requiredOnly = false;
  importedData = new Map<string, any>()

  constructor(
    private _fb: UntypedFormBuilder,
    private _ocsfService: OcsfService,
    private _chainPageService: ChainPageService,
    private _messageService: NzMessageService,
    private _store: Store<LiveViewState>) {

    this.results$ = this._store.pipe(select(getResults));
  }

  ngOnInit(): void {
    this.filtersForm = this._fb.group({
      dataClassFilters: [null],
      dataClasses: [null]
    });
    this.ocsfForm = this._fb.group({
      _filePath: "",
      _sourceName: ""
    })

    this.filtersForm.get('dataClassFilters')!.valueChanges.subscribe(selectedObject => {
      this.onDataClassFiltersChange(selectedObject);
    });

    this.filtersForm.get('dataClasses')!.valueChanges.subscribe(selectedObject => {
      this.onDataClassesChange(selectedObject);
    });
  }

  onDataClassFiltersChange(selectedObjectList: string[]): void {
    this.selectedClassFilters = selectedObjectList
  }

  onDataClassesChange(selectedObjectList: string[]): void {
    this.selectedClassFields = selectedObjectList
  }

  clickSwitch() {
    this.requiredOnly = !this.requiredOnly;
  }

  onOcsfSubmit() {
    const columnMappings: IndexingColumnMapping[] = []
    this.collectFormsValuesToIndexMappings(this.ocsfForm, columnMappings, null)

    const tableMapping: IndexTableMapping = {
      table_name: "ocsf",
      column_mapping: columnMappings
    }

    let sourceName = this.ocsfForm.value._sourceName;
    if (sourceName == null || sourceName === "") {
      sourceName = "squid"
    }
    const result: { [key: string]: IndexTableMapping } = {};

    result[sourceName] = tableMapping
    this._chainPageService.saveIndexMappings({
      filePath: this.ocsfForm.value._filePath,
      mappings: result
    }).subscribe(() =>
      this._messageService.create('success', 'Successfully saved the OCSF schema'))
  }

  onOcsfImport() {
    this._chainPageService.getIndexMappings({filePath: this.ocsfForm.value._filePath})
      .subscribe((response: HttpResponse<{ path: string, result: { [key: string]: any } }>) => {
        if (response.status === 200) {
          let sourceName = this.ocsfForm.value._sourceName;
          if (sourceName == null || sourceName === "") {
            sourceName = Object.keys(response.body.result)[0]
          }

          const filePath = response.body.path;
          this.ocsfForm.get("_filePath").setValue(filePath);
          this.ocsfForm.get("_sourceName").setValue(sourceName);

          const mappings: IndexingColumnMapping[] = response.body.result[sourceName].column_mapping;
          if (mappings.length === 0) {
            this._messageService.create('warning', `No indexing fields found for the given source name '${sourceName}'`);
            return
          }
          this.fillFormsValuesFromIndexMappings(null, mappings)
          this._messageService.create('success', `Imported the OCSF schema from '${filePath}', source: '${sourceName}'`);
        } else if (response.status === 204 || response.status === 404) {
          this._messageService.create('warning', `No indexing fields found for the given path '${this.ocsfForm.value._filePath}'`);
        }
        return {
          path: '', result: {}
        };
      })
  }

  fillImportedData(importMap: Map<string, any>, fieldHierarchy: string[], value: string, index: number) {
    const currentField = fieldHierarchy[index];
    if (fieldHierarchy.length === index + 1) {
      importMap.set(currentField, value)
    } else {
      if (!importMap.has(currentField)) {
        importMap.set(currentField, new Map<string, any>())
      }
      this.fillImportedData(importMap.get(currentField), fieldHierarchy, value, index + 1)
    }
  }

  fillFormsValuesFromIndexMappings(form: UntypedFormGroup, mappings: IndexingColumnMapping[]) {
    const classNames = new Set();
    for (const columnMapping of mappings) {
      const fieldHierarchy = columnMapping.name.split(".");
      classNames.add(fieldHierarchy[0])
      this.fillImportedData(this.importedData, fieldHierarchy, columnMapping.kafka_name, 0)
    }
    this.filtersForm.get('dataClasses')!.setValue(Array.from(classNames) as string[])
  }

  collectFormsValuesToIndexMappings(form: UntypedFormGroup, result: IndexingColumnMapping[], path: string) {
    for (const key in form.controls) {
      if (key.startsWith("_")) {
        continue
      }
      const control = form.get(key);
      const keyPath = path == null ? key : path + "." + key
      if (control instanceof UntypedFormGroup) {
        this.collectFormsValuesToIndexMappings(control, result, keyPath)
      } else {
        if (control.value !== null && control.value !== undefined && control.value !== '') {
          result.push({
            name: keyPath,
            kafka_name: control.value,
            path: "."
          })
        }
      }
    }
  }

  filterClasses(classes: { [className: string]: BaseOcsfSchemaModel }): { [className: string]: BaseOcsfSchemaModel } {
    if (this.selectedClassFilters.length <= 0) {
      return classes
    }
    return Object.fromEntries(
      Object.entries(classes)
        .filter(([, value]) =>
          this.selectedClassFilters.every(profile => value.profiles.includes(profile))
        )
    );
  }
}
