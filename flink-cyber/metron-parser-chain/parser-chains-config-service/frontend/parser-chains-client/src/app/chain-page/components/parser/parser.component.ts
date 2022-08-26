import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import produce from 'immer';
import { get } from 'lodash';
import { set } from 'lodash';

import { ParserModel } from '../../chain-page.models';
import { CustomFormConfig } from '../custom-form/custom-form.component';

import { ConfigChangedEvent } from './advanced-editor/advanced-editor.component';

@Component({
  selector: 'app-parser',
  templateUrl: './parser.component.html',
  styleUrls: ['./parser.component.scss']
})
export class ParserComponent implements OnInit, OnChanges {

  @Input() collapsed: boolean;
  @Input() dirty = false;
  @Input() parser: ParserModel;
  @Input() configForm: CustomFormConfig[];
  @Input() isolatedParserView = false;
  @Input() parserType: string;
  @Input() failedParser: string;
  @Output() removeParser = new EventEmitter<string>();
  @Output() parserChange = new EventEmitter<any>();

  editName = false;

  areFormsReadyToRender = false;
  parserCollapseState = [];

  get parsingFailed() {
    return this.failedParser === this.parser.id;
  }

  ngOnInit() {
    this.configForm = this.setFormFieldValues(this.configForm);


    setTimeout(() => {
      this.areFormsReadyToRender = true;
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.configForm) {
      this.areFormsReadyToRender = false;
      this.configForm = this.setFormFieldValues(this.configForm);
      setTimeout(() => {
        this.areFormsReadyToRender = true;
      });
    }
    if (changes.parser && changes.parser.previousValue) {
      Object.keys(changes.parser.previousValue).forEach(key => {
        if (changes.parser.previousValue[key] !== changes.parser.currentValue[key]) {
          if (key === 'config') {
            this.configForm = this.updateFormValues('config', this.configForm);
          }
        }
      });
    }
  }

  updateFormValues(key:string, fields:any = []) : any {
    return produce(fields, (draft) => {
      draft.forEach(field => {
        if (field.path && field.path.split('.')[0] === key) {
          let value;
          if (field.multiple !== true) {
            value = get(this.parser, field.path + '.' + field.name);
          } else {
            value = get(this.parser, field.path);
            value = Array.isArray(value) ? value.filter(Boolean).map((item) => {
              if (item[field.name]) {
                return {
                  ...item,
                  [field.name]: item[field.name]
                };
              }
              return item;
            }) : Array.isArray(field.defaultValue) ? field.defaultValue.map((item) => {
              return { [field.name]: item[field.name] || '' };
            }) : [];
          }
          field.value = value || field.defaultValue || '';
        } else if (field.name === key) {
          field.value = this.parser[key] || field.defaultValue || '';
        }
      });
    });
  }

  setFormFieldValues(fields:any = []) : any {
    return produce(fields, (draft : any) => {
      draft.forEach((field : any) => {
        field.id = [
          this.parser.id,
          field.path ? [field.path, field.name].join('.') : field.name
        ].join('-');
        if (field.path) {
          let value;
          if (field.multiple !== true) {
            value = get(this.parser, [field.path, field.name].join('.'));
          } else {
            value = get(this.parser, field.path);
            value = Array.isArray(value) ? value.filter(Boolean).map((item) => {
              if (item[field.name]) {
                return {
                  ...item,
                  [field.name]: item[field.name]
                };
              }
              return item;
            }) : Array.isArray(field.defaultValue) ? field.defaultValue.map((item) => {
              return { [field.name]: item[field.name] || '' };
            }) : [];
          }
          field.value = value || field.defaultValue || '';
        } else {
          field.value = this.parser[field.name] || field.defaultValue || '';
        }
      });
    });
  }

  private updateRegularParserWithFormData(parser: ParserModel, formFieldData: CustomFormConfig) {
    return formFieldData.path
      ? produce(parser, (draftParser) => {
          set({
            ...draftParser
          }, [formFieldData.path, formFieldData.name].join('.'), formFieldData.value);
        })
      : {
        id: parser.id,
        [formFieldData.name]: formFieldData.value
      };
  }

  private updateMultiValueParserWithFormData(parser: ParserModel, formFieldData: CustomFormConfig) {
    let current;
    if (formFieldData.path) {
      current = get(parser, formFieldData.path);
    } else {
      current = parser[formFieldData.name];
    }
    if (!current) {
      return produce(parser, (draftParser) => {
        if (formFieldData.path) {
          set({
            ...draftParser
          }, formFieldData.path, formFieldData.value || formFieldData.defaultValue);
        } else {
          draftParser[formFieldData.name] = formFieldData.value || formFieldData.defaultValue;
        }
      });
    } else {
      const newValue = formFieldData.value;
      const loopThrough = newValue.length > current.length
        ? newValue
        : current;
      current = loopThrough.map((item, i) => {
        if (current[i] && newValue[i]) {
          return {
            ...current[i],
            ...newValue[i]
          };
        } else if (!newValue[i] && current[i]) {
          const newItem = { ...current[i] };
          delete newItem[formFieldData.name];
          if (Object.keys(newItem).length === 0) {
            return null;
          }
          return newItem;
        } else if (newValue[i] && !current[i]) {
          return newValue[i];
        } else if (!newValue[i] && !current[i]) {
          return null;
        }
        return item;
      }).filter(Boolean);
      return produce(parser, (draftParser) => {
        if (formFieldData.path) {
          set({
            ...draftParser
          }, formFieldData.path, current);
        } else {
          draftParser[formFieldData.name] = current;
        }
      });
    }
  }

  onCustomFormChange(parser: ParserModel, formFieldData: CustomFormConfig) {
    let partialParser;
    if (formFieldData.multiple) {
      partialParser = this.updateMultiValueParserWithFormData(parser, formFieldData);
    } else {
      partialParser = this.updateRegularParserWithFormData(parser, formFieldData);
    }

    this.dirty = true;
    this.parserChange.emit(partialParser);
  }

  onRemoveParser(parserId: string) {
    this.removeParser.emit(parserId);
  }

  onAdvancedEditorChanged(event: ConfigChangedEvent) {
    this.parserChange.emit({ id: this.parser.id, config: event.value });
  }

  onParserNameChange(name: string) {
    this.parserChange.emit({ id: this.parser.id, name });
  }

  preventCollapse(event: Event) {
    event.stopPropagation();
  }

}
