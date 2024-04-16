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

import { Component } from '@angular/core';
import {Router} from "@angular/router";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'Parser Chaining';
  isReverseArrow = false;
  width: string | number = 200;
  isCollapsed = false;
  isOpen: Array<boolean> = [false, false];

  constructor(private _router: Router) {
  }

  getTitle() {
    if (this._router.url.startsWith('/cluster')) {
      return 'Cluster Management';
    }
    else if (this._router.url.startsWith('/parserconfig')) {
      return 'Parser Chaining';
    }
    return '';
  }

  stop(event: Event) {
    event.stopPropagation();
  }
}
