import {Component, OnInit} from '@angular/core';
import {BehaviorSubject, Observable, of} from "rxjs";
import {catchError, take, tap} from "rxjs/operators";
import {OcsfService} from "../../../services/ocsf.service";
import {BaseOcsfSchemaModel, OcsfSchemaModel} from "./ocsf-form.model";
import {UntypedFormBuilder, UntypedFormGroup} from "@angular/forms";
import {EntryParsingResultModel} from "../live-view/models/live-view.model";
import {select, Store} from "@ngrx/store";
import {LiveViewState} from "../live-view/live-view.reducers";
import {getResults} from "../live-view/live-view.selectors";
import {IndexingColumnMapping, IndexTableMapping, TableColumnDto} from "../../chain-page.models";
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
      _mappingFilePath: "",
      _tableFilePath: "",
      _sourceName: ""
    })

    this.filtersForm.get('dataClassFilters')!.valueChanges.subscribe(selectedObject => {
      this.selectedClassFilters = selectedObject
    });

    this.filtersForm.get('dataClasses')!.valueChanges.subscribe(selectedObject => {
      this.selectedClassFields = selectedObject
    });
  }

  clickSwitch() {
    this.requiredOnly = !this.requiredOnly;
  }

  onOcsfSubmit() {
    const columnMappings: IndexingColumnMapping[] = []
    const tableColumnList: TableColumnDto[] = [];
    this.collectFormsValuesToIndexMappings(this.ocsfForm, columnMappings, tableColumnList)

    const tableMapping: IndexTableMapping = {
      table_name: "ocsf",
      column_mapping: columnMappings
    }

    const tableConfig = {
      "ocsf": tableColumnList
    }

    let sourceName = this.ocsfForm.value._sourceName;
    if (sourceName == null || sourceName === "") {
      sourceName = "squid"
    }
    const mappings: { [key: string]: IndexTableMapping } = {};

    mappings[sourceName] = tableMapping
    this._chainPageService.saveIndexMappings({
      tableFilePath: this.ocsfForm.value._tableFilePath,
      mappingFilePath: this.ocsfForm.value._mappingFilePath,
      mappings,
      tableConfig
    })
      .pipe(take(1))
      .subscribe(() =>
        this._messageService.create('success', 'Successfully saved the mapping config and table config files for OCSF schema!'))
  }

  onOcsfImport() {
    this._chainPageService.getIndexMappings({filePath: this.ocsfForm.value._mappingFilePath})
      .pipe(
        catchError(_ => {
          this._messageService.create('error', `Couldn't fetch indexing fields for the given path '${this.ocsfForm.value._mappingFilePath}'`)
          return of(null)
        }),
        take(1)
      )
      .subscribe((mappingResponse: HttpResponse<{ path: string, result: { [key: string]: any } }>) => {
        if (mappingResponse.status === 200) {
          let sourceName = this.ocsfForm.value._sourceName;
          if (sourceName == null || sourceName === "") {
            sourceName = Object.keys(mappingResponse.body.result)[0]
          }

          const mappingsFilePath = mappingResponse.body.path;

          const mappings: IndexingColumnMapping[] = mappingResponse.body.result[sourceName].column_mapping;
          if (mappings.length === 0) {
            this._messageService.create('warning', `No indexing fields found for the given source name '${sourceName}'`);
            return
          }
          // table config fetch
          this._chainPageService.getIndexTableConfig({filePath: this.ocsfForm.value._tableFilePath})
            .pipe(
              catchError(_ => {
                this._messageService.create('error', `Couldn't fetch indexing table for the given path '${this.ocsfForm.value._tableFilePath}'`)
                return of(null)
              }),
              take(1)
            )
            .subscribe((tableResponse: HttpResponse<{ path: string, result: { [key: string]: TableColumnDto[] } }>) => {
              if (tableResponse.status === 200) {

                const tableFilePath = tableResponse.body.path;
                this.ocsfForm.get("_tableFilePath").setValue(tableFilePath);
                this.ocsfForm.get("_mappingFilePath").setValue(mappingsFilePath);
                this.ocsfForm.get("_sourceName").setValue(sourceName);

                const tableConfig = tableResponse.body.result.ocsf;

                this.fillFormsValuesFromIndexMappings(mappings, tableConfig)
                this._messageService.create('success', `Imported the OCSF schema from '${mappingsFilePath}' and '${tableFilePath}', source: '${sourceName}'`);
              } else if (tableResponse.status === 204) {
                this._messageService.create('warning', `No indexing table found for the given path '${this.ocsfForm.value._mappingFilePath}'`);
              }
            })
        } else if (mappingResponse.status === 204) {
          this._messageService.create('warning', `No indexing fields found for the given path '${this.ocsfForm.value._mappingFilePath}'`);
        }
        return {
          path: '', result: {}
        };
      })
  }

  objectToMap(obj: any): Map<string, any> {
    const result = new Map<string, any>();

    for (const [key, value] of Object.entries(obj)) {
      if (value && typeof value === "object" && !Array.isArray(value)) {
        result.set(key, this.objectToMap(value));
      } else {
        result.set(key, value);
      }
    }

    return result;
  }

  fillFormsValuesFromIndexMappings(mappings: IndexingColumnMapping[], tableConfig: TableColumnDto[]) {
    const classNameSet = new Set();
    for (const columnMapping of mappings) {
      const className = columnMapping.name;
      classNameSet.add(className)
      for (const tableColumnDto of tableConfig) {
        if (tableColumnDto.name === className) {
          let structString = tableColumnDto.type
          const valuesString = columnMapping.kafka_name

          structString = structString.replace(/struct</g, "{")
          structString = structString.replace(/>/g, "}")
          structString = structString.replace(/`/g, "\"")

          for (const valueName of valuesString.split(",")) {
            structString = structString.replace(": string", ": \"" + valueName + "\"")
          }
          this.importedData.set(className, this.objectToMap(JSON.parse(structString)))
          break
        }
      }
    }
    this.filtersForm.get('dataClasses')!.setValue(Array.from(classNameSet) as string[])
  }

  collectFormsValuesToIndexMappings(form: UntypedFormGroup, tableMapping: IndexingColumnMapping[], tableConfig: TableColumnDto[]) {
    for (const ocsfObject in form.controls) {
      if (ocsfObject.startsWith("_")) {
        continue
      }
      const tableColumn: TableColumnDto = {
        name: ocsfObject,
        type: "",
        nullable: false,
      }
      tableConfig.push(tableColumn)
      const columnMapping: IndexingColumnMapping = {
        name: ocsfObject,
        kafka_name: "",
        transformation: "",
        path: "."
      }
      tableMapping.push(columnMapping)

      this.collectFormValuesRecursive(form.get(ocsfObject) as UntypedFormGroup, columnMapping, tableColumn)
    }
  }

  collectFormValuesRecursive(form: UntypedFormGroup, columnMapping: IndexingColumnMapping, tableColumn: TableColumnDto) {
    tableColumn.type = tableColumn.type + "struct<"

    columnMapping.transformation = columnMapping.transformation === ""
      ? columnMapping.transformation + "ROW("
      : columnMapping.transformation + ", ROW("

    let i = 0;
    for (const key in form.controls) {
      if (Object.prototype.hasOwnProperty.call(form.controls, key)) {
        const control = form.get(key);
        if (control instanceof UntypedFormGroup) {
          tableColumn.type = i === 0
            ? tableColumn.type + "`" + key + "`: "
            : tableColumn.type + ", `" + key + "`: "
          this.collectFormValuesRecursive(control, columnMapping, tableColumn)
          i++;
        } else {
          if (control.value !== null && control.value !== undefined && control.value !== '') {
            if (i > 0) {
              tableColumn.type = tableColumn.type + ", "
              columnMapping.transformation = columnMapping.transformation + ", "
            }
            tableColumn.type = tableColumn.type + "`" + key + "`: string"
            columnMapping.transformation = columnMapping.transformation + "%s"
            columnMapping.kafka_name = columnMapping.kafka_name === ""
              ? control.value
              : columnMapping.kafka_name + "," + control.value
            i++;
          }
        }
      }
    }
    tableColumn.type = tableColumn.type + ">"
    columnMapping.transformation = columnMapping.transformation + ")"

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
