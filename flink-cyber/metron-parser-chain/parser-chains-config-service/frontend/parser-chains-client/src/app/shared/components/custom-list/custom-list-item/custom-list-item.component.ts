import {
  AfterContentInit,
  Component,
  ContentChild,
  ContentChildren,
  Directive,
  ElementRef, EventEmitter, HostBinding,
  inject,
  Input,
  OnDestroy, Output,
  QueryList, ViewEncapsulation
} from '@angular/core';
import {CustomListComponent} from 'src/app/shared/components/custom-list/custom-list.component';
import {Subject} from 'rxjs';
import {setLines} from '@angular/material/core';
import {BooleanInput, coerceBooleanProperty} from '@angular/cdk/coercion';

@Directive({
  selector: '[app-custom-list-line], [customListLine]',
})
export class CustomListLineDirective {
  @HostBinding('class') class = 'custom-list-line'
}

@Directive({
  selector: '[app-custom-list-chips], [customListChips]',
})
export class CustomListChipsDirective {
  @HostBinding('class') class = 'custom-list-chips'
}

@Directive({
  selector: '[app-custom-list-action], [customListAction]',
})
export class CustomListActionDirective {
  @HostBinding('class') class = 'custom-list-actions'
}

@Directive({
  selector: '[app-custom-list-icon], [customListIcon]',
})
export class CustomListIconDirective {
  @HostBinding('class') class = 'custom-list-icon'
}

@Directive({
  selector: '[app-custom-list-subheader], [customListSubheader]',
})
export class CustomListSubheaderDirective {
  @HostBinding('class') class = 'mat-subheader flex-row-jfs-ac'
}

@Component({
  selector: 'app-custom-list-line',
  templateUrl: './custom-list-item.component.html',
  styleUrls: ['./custom-list-item.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class CustomListItemComponent implements AfterContentInit, OnDestroy {
  private readonly _destroyed = new Subject<void>();
  private _list?: CustomListComponent;
  private _element: ElementRef<HTMLElement> = inject(ElementRef);
  @ContentChild(CustomListIconDirective) private _icon: CustomListIconDirective;
  @ContentChildren(CustomListLineDirective, {descendants: true}) private _lines: QueryList<CustomListLineDirective>;
  @ContentChildren(CustomListChipsDirective, {descendants: true})private _chips: QueryList<CustomListChipsDirective>;
  @ContentChildren(CustomListActionDirective,  {descendants: true})private _actions: QueryList<CustomListActionDirective>;

  @Input() itemId: string;
  @Input() chips1: string[];
  @Input() chips2: string[];
  @Output() chips1RemoveEmitter = new EventEmitter<string>();
  @Output() chips2RemoveEmitter = new EventEmitter<string>();

  @HostBinding('class') baseClass = 'custom-list-item mat-focus-indicator';
  @HostBinding('class.custom-list-item-disabled') get isDisabled() {
    return this.disabled;
  }
  @HostBinding('class.custom-list-item-with-avatar') get hasIcon() {
    return this._icon;
  }

  /** Whether the option is disabled. */
  @Input()
  get disabled(): boolean {
    return this._disabled || !!(this._list && this._list.disabled);
  }
  set disabled(value: BooleanInput) {
    this._disabled = coerceBooleanProperty(value);
  }
  private _disabled = false;

  ngAfterContentInit() {
    setLines(this._lines, this._element);
  }

  ngOnDestroy() {
    this._destroyed.next();
    this._destroyed.complete();
  }

  remove(emitter: EventEmitter<string>, chip: string) {
    emitter.emit(chip);
  }
}
