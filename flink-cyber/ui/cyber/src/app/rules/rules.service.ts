import { Injectable } from '@angular/core';
import { EntityCollectionServiceBase, EntityCollectionServiceElementsFactory } from '@ngrx/data';
import { Rule } from './rule.model';

@Injectable({
  providedIn: 'root'
})
export class RulesService extends EntityCollectionServiceBase<Rule> {
  constructor(elementsFactory: EntityCollectionServiceElementsFactory) {
    super('Rule', elementsFactory);
  }
}
