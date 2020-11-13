import { NgModule } from '@angular/core';

import { RulesRoutingModule } from './rules-routing.module';

import { RulesListComponent } from './rules-list/rules-list.component';
import { RulesEditorComponent } from './rules-editor/rules-editor.component';
import { RulesComponent } from './rules.component';
import { RulesService } from './rules.service';
import { EntityServices } from '@ngrx/data';


@NgModule({
  imports: [RulesRoutingModule],
  declarations: [RulesListComponent, RulesEditorComponent, RulesComponent],
  exports: [RulesListComponent],
  providers: [RulesService]
})
export class RulesModule {
  constructor(entityServices: EntityServices, rulesService: RulesService) {
    entityServices.registerEntityCollectionServices([rulesService]);
  }
}
