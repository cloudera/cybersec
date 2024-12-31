import {Component, EventEmitter, Input, Output, ViewEncapsulation} from '@angular/core';
import {MatChip} from '@angular/material/chips';

@Component({
  selector: 'app-styled-chips-list',
  templateUrl: './styled-chips-list.component.html',
  styleUrls: ['./styled-chips-list.component.scss'],
  encapsulation: ViewEncapsulation.Emulated, // Default setting
})
export class StyledChipsListComponent {
  @Input() chips: CustomChip[];
  @Input() color: 'success' | 'primary' = 'primary';
  @Input() forceStyleDisplay = false;
  @Input() selectable = false;
  @Output() removeEmitter = new EventEmitter<string>();
  @Output() selectionField = new EventEmitter<CustomChip>();

  inputClass(chip: CustomChip) {
    if (!chip.selected && !this.forceStyleDisplay) {
      return 'unselected-chip';
    }
    return this.color + '-chip';
  }

  remove(chip: string) {
    this.removeEmitter.emit(chip);
  }

  click(chipHash: MatChip, name: string) {
    if (this.selectable) {
      this.chips = this.chips.map(c => {
        if (c.name === name) {
          console.log(chipHash);
          this.selectionField.emit(c);

          return {...c, selected: !c.selected};
        }
        return c;
      })
    }
  }


}

export type CustomChip = {
  name: string;
  allowMapping: boolean;
  selected: boolean;
  mappingValue?: string;
  removable: boolean;
}
