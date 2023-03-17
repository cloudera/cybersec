import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {ConfigChangedEvent} from "../parser/advanced-editor/advanced-editor.component";

@Component({
  selector: 'app-indexing-form',
  templateUrl: './indexing-form.component.html',
  styleUrls: ['./indexing-form.component.scss']
})
export class IndexingFormComponent implements OnInit {

  @Output() fieldSetUpdated = new EventEmitter<Map<string,boolean>>()

  constructor() {
  }

  ngOnInit(): void {
  }

  onAdvancedEditorChanged(e: ConfigChangedEvent) {
    let result = new Map<string, boolean>()

    this.findValues(e,'ignore_fields').forEach(ignore_list =>
        ignore_list.forEach(value => result.set(value, true)));

    this.findValues(e,'column_mapping').forEach(mapping =>
        this.findValues(mapping, 'name').forEach(value => result.set(value, false)))

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

}
