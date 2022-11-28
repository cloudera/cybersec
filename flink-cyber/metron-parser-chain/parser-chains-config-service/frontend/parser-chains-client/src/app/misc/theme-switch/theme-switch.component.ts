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

import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-theme-switch',
  templateUrl: './theme-switch.component.html',
  styleUrls: ['./theme-switch.component.scss']
})
export class ThemeSwitchComponent implements OnInit {
  isDarkMode = false;

  ngOnInit() {
    this.isDarkMode = window.localStorage.getItem('dark-mode') === 'on';
    this.onChange(this.isDarkMode);
  }

  onChange(value) {
    document.body.classList[value ? 'add' : 'remove']('dark-mode');
    this.isDarkMode = value;
    window.localStorage.setItem('dark-mode', value ? 'on' : 'off');
  }
}
