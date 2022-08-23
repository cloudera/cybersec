import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzSwitchModule } from 'ng-zorro-antd';

import { ThemeSwitchComponent } from './theme-switch.component';

@NgModule({
  declarations: [
    ThemeSwitchComponent
  ],
  imports: [
    CommonModule,
    NzSwitchModule,
    FormsModule
  ],
  exports: [
    ThemeSwitchComponent
  ]
})
export class ThemeSwitchModule { }
