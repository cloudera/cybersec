import {Component, EventEmitter, Output} from '@angular/core';
import {ConfigChangedEvent} from "../parser/advanced-editor/advanced-editor.component";
import {UntypedFormBuilder, UntypedFormGroup} from "@angular/forms";
import {catchError, map, switchMap, tap} from "rxjs/operators";
import {of, Subject} from "rxjs";
import {findValues} from "src/app/shared/utils";
import {ChainPageService} from "src/app/services/chain-page.service";
import {NzMessageService} from "ng-zorro-antd/message";
import {HttpResponse} from "@angular/common/http";

@Component({
  selector: 'app-indexing-form',
  templateUrl: './indexing-form.component.html',
  styleUrls: ['./indexing-form.component.scss']
})
export class IndexingFormComponent {
  @Output() fieldSetUpdated = new EventEmitter<{ [key: string]: { [key: string]: boolean } }>();
  form: UntypedFormGroup = this._fb.group({filePath: ''});
  subjectMappingPath$ = new Subject<string>();
  mappingJson$ = this.subjectMappingPath$.pipe(
    switchMap((value) => this._chainPageService.getIndexMappings({filePath: value})),
    catchError(err => {
      this._messageService.create('Error', 'Error fetching indexing fields');
      return of(null);
    }),
    map((response: HttpResponse<{ path: string, result: { [key: string]: object } }>) => {
      switch (response.status) {
        case 200:
          return {
            path: response.body.path, result: response.body.result
          };
        case 204:
        case 404:
          this._messageService.create('warning', `No indexing fields found for the given path '${this.form.value.filePath}'`);
          return {
            path: '', result: {}
          };
        default:
          return null;
      }
    }),
    tap((response) => {
      if (response) {
        this.onAdvancedEditorChanged({value: response.result});
      }
    })
  );

  constructor(private _fb: UntypedFormBuilder,
              private _chainPageService: ChainPageService,
              private _messageService: NzMessageService) {
  }

  onAdvancedEditorChanged(e: ConfigChangedEvent) {
    const data = e.value;
    if (data === null || Object.keys(data).length === 0) {
      return;
    }
    const fieldSet = Object.entries(data).reduce((result, [key, value]) => {
      const ignoreNames = findValues<string>(value, 'ignore_fields').flat().reduce((acc, ignoreName) =>
        Object.assign(acc, {[ignoreName]: true}), {} as { [key: string]: boolean });
      const fieldNames = findValues<object>(value, 'column_mapping').flatMap(arr => findValues<string>(arr, 'name')).flat().reduce((acc, fieldName) => Object.assign(acc, {[fieldName]: false}), {} as {
        [key: string]: boolean
      });
      if (Object.keys(fieldNames).length === 0 && Object.keys(ignoreNames).length === 0) {
        return result;
      }
      return Object.assign(result, {[key]: {...ignoreNames, ...fieldNames}});
    }, {} as { [key: string]: { [key: string]: boolean } });
    this.fieldSetUpdated.emit(fieldSet);
  }
}
