import {Component, Input, OnInit} from '@angular/core';
import {BaseOcsfSchemaModel, BaseOcsfTypeModel, OcsfSchemaModel} from "../ocsf-form.model";
import {EntryParsingResultModel} from "../../live-view/models/live-view.model";
import {AbstractControl, FormControl, FormGroup, UntypedFormGroup, ValidatorFn, Validators} from "@angular/forms";

@Component({
  selector: 'app-ocsf-object-form',
  templateUrl: './ocsf-object-form.component.html',
  styleUrls: ['./ocsf-object-form.component.scss'],
  // changeDetection: ChangeDetectionStrategy.OnPush
})
export class OcsfObjectFormComponent implements OnInit {

  @Input() schema: OcsfSchemaModel;
  @Input() currentObject: BaseOcsfSchemaModel;
  @Input() liveResults: EntryParsingResultModel[];
  @Input() requiredOnly: boolean;
  @Input() formRequired: boolean;
  @Input() parentFormGroup!: UntypedFormGroup;
  @Input() importedData?: Map<string, any>;
  protected childForm: UntypedFormGroup;
  protected openedChildObjects: { [key: string]: boolean } = {};
  protected formChanged = false;

  constructor() {
  }

  ngOnInit(): void {
    this.childForm = new FormGroup({});
    this.parentFormGroup.addControl(this.currentObject.name, this.childForm);

    Object.keys(this.currentObject.attributes).forEach((attributeKey) => {
      const attribute = this.currentObject.attributes[attributeKey];
      if (attribute.object_type) {
        return;
      }
      const value = this.importedData?.get(attributeKey);
      const formControl = new FormControl(value === undefined ? null : value, []);
      this.calculateValidators(formControl, attribute);
      this.childForm.addControl(attributeKey, formControl);
    })
  }

  onCollapseChange(attribute, caption) {
    if (this.openedChildObjects[caption] == null) {
      this.openedChildObjects[caption] = true;
    }
  }

  onInputChange(e) {
    if (this.formRequired) {
      return
    }
    let formEmpty = false
    if (e.target.value !== "") {
      formEmpty = false;
    } else if (this.formChanged && this.isFormEmpty(this.childForm)) {
      formEmpty = true
    }
    if (formEmpty === this.formChanged) {
      this.formChanged = !this.formChanged
      this.triggerValidation(this.childForm, this.currentObject)
    }
  }

  calculateValidators(control: AbstractControl, attribute: BaseOcsfTypeModel): void {
    if (!this.formRequired && !this.formChanged) {
      control.setValidators(null)
    }
    const validators: ValidatorFn[] = []

    if (attribute.requirement === 'required') {
      validators.push(Validators.required)
    }
    //TODO should be based on the data type of the sample. Do we even need this?
    // validators.push(this.getValidatorsForType(attribute.type))

    control.setValidators(validators)
  }

  triggerValidation(formGroup: UntypedFormGroup, parentObject: BaseOcsfSchemaModel): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      const attribute = parentObject.attributes[key];
      if (control instanceof UntypedFormGroup) {
        this.triggerValidation(control, this.schema.objects[attribute.object_type])
      } else {
        this.calculateValidators(control, attribute)
        control.updateValueAndValidity();
      }
    })
  }

  isFormEmpty(formGroup: UntypedFormGroup): boolean {
    let result = true
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      if (control instanceof UntypedFormGroup) {
        if (!this.isFormEmpty(control)) {
          result = false;
        }
      } else {
        if (control.value !== null && control.value !== undefined && control.value !== '') {
          result = false;
        }
      }
    })
    return result;
  }

  private _getValidatorsForType(typeName: string) {
    const typeObj = this.schema.types[typeName];
    if (typeObj == null) {
      console.warn("Unknown type ", typeName);
      return null;
    }
    const validators: ValidatorFn[] = []

    if (typeObj.max_len) {
      validators.push(Validators.maxLength(typeObj.max_len));
    }
    if (typeObj.regex) {
      validators.push(Validators.pattern(typeObj.regex));
    }
    if (typeObj.values) {
      validators.push(Validators.pattern(typeObj.values.join("|")));
    }
    if (typeObj.range) {
      //TODO
    }
    if (validators.length > 0) {
      return validators;
    }

    switch (typeName) {
      case "float_t": {
        return Validators.pattern("[+-]?([0-9]*[.])?[0-9]+")
      }
      case "integer_t":
      case "long_t": {
        return Validators.pattern("[+-]?[0-9]+")
      }
      default: {
        return this._getValidatorsForType(typeObj.type)
      }
    }
  }
}
