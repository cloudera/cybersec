import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';

import { DeactivatePreventer } from './deactivate-preventer.interface';

@Injectable()
export class CanDeactivateComponent implements CanDeactivate<DeactivatePreventer> {
  canDeactivate(component: DeactivatePreventer) {
    return component.canDeactivate();
  }
}
