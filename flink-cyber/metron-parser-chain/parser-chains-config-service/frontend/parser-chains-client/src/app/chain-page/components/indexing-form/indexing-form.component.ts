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
  @Output() fieldSetUpdated = new EventEmitter<{[key: string]: {[key:string] : boolean}}>();
  form: UntypedFormGroup = this._fb.group({filePath: ''});
  subjectMappingPath$ = new Subject<string>();
  mappingJson$= this.subjectMappingPath$.pipe(
    switchMap((value) =>  this._chainPageService.getIndexMappings({filePath: value})),
    catchError(err => {
      this._messageService.create('Error', 'Error fetching indexing fields');
      return of(null);
    }),
    map((response: HttpResponse<{ path: string, result: {[key:string]: object} }>) => {
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
    const result = Object.entries(e.value).reduce((acc,[key, value]) => {
      const sourceMap = {} as {[key:string] : boolean};
      findValues<string[]>(value, 'ignore_fields').forEach(ignoreList =>
        ignoreList.forEach((ignoreName) => Object.assign(sourceMap, {[ignoreName]: true})));
      findValues<object>(value, 'column_mapping').forEach(mapping =>
        findValues<string>(mapping, 'name').forEach(name => Object.assign(sourceMap, {[name]: false})));
      return Object.assign(acc, {[key]: sourceMap});
    }, {} as {[key: string]: {[key:string] : boolean}});
    this.fieldSetUpdated.emit(result);
  }
}
