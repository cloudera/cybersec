/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

import {NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {ChainListPageComponent} from './chain-list-page.component';
import {SharedModule} from 'src/app/shared/share.module';
import {MatCardModule} from '@angular/material/card';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatTableModule} from '@angular/material/table';
import {MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {AsyncPipe, NgComponentOutlet, NgForOf, NgIf, NgTemplateOutlet} from '@angular/common';
import {ChainDialogComponent} from 'src/app/chain-list-page/component/chain-dialog.component';
import {MatSelectModule} from '@angular/material/select';
import {MatAutocompleteModule} from '@angular/material/autocomplete';

@NgModule({
  declarations: [ChainListPageComponent, ChainDialogComponent],
  imports: [
    ReactiveFormsModule,
    RouterModule,
    SharedModule,
    MatCardModule,
    MatDividerModule,
    MatTooltipModule,
    MatTableModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    NgIf,
    NgTemplateOutlet,
    NgComponentOutlet,
    AsyncPipe,
    MatSelectModule,
    FormsModule,
    NgForOf,
    MatAutocompleteModule
  ],
  providers: [],
  exports: [ChainListPageComponent]
})
export class ChainListPageModule { }
