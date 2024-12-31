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

import {ComponentFixture, TestBed, waitForAsync} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {DeleteFill} from '@ant-design/icons-angular/icons';
import {NzModalModule} from 'ng-zorro-antd/modal';
import {NZ_ICONS} from 'ng-zorro-antd/icon';

import {RouterComponent} from './router.component';
import {NzCollapseModule} from "ng-zorro-antd/collapse";
import {NzTabsModule} from "ng-zorro-antd/tabs";
import {NzFormModule} from "ng-zorro-antd/form";
import {MockComponent} from "ng-mocks";
import {CustomFormComponent} from "../custom-form/custom-form.component";


describe('RouterComponent', () => {
  let component: RouterComponent;
  let fixture: ComponentFixture<RouterComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NzModalModule,
        NoopAnimationsModule,
        NzCollapseModule,
        NzTabsModule,
        NzFormModule,
        FormsModule,
      ],
      declarations: [
        MockComponent(CustomFormComponent),
        RouterComponent
      ],
      providers: [
        {provide: NZ_ICONS, useValue: [DeleteFill]}
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RouterComponent);
    component = fixture.componentInstance;
    component.parser = {
      id: '123',
      name: 'Some parser',
      type: 'Bro'
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit a parser change', () => {
    const spy = spyOn(component.parserChange, 'emit');
    component.onMatchingFieldBlur(({
      target: {
        value: ' trim me!     '
      }
    } as unknown) as Event, {
      id: '123',
      name: 'parser',
      type: 'foo',
      routing: {
        lorem: 'ipsum'
      }
    });
    expect(spy).toHaveBeenCalledWith({
      id: '123',
      routing: {
        lorem: 'ipsum',
        matchingField: 'trim me!'
      }
    });
  });

  it('should emit subchain select', () => {
    const spy = spyOn(component.subchainSelect, 'emit');
    component.onSubchainClick('777');
    expect(spy).toHaveBeenCalledWith('777');
  });

  it('should emit route add', () => {
    const spy = spyOn(component.routeAdd, 'emit');
    const event = ({
      preventDefault: jasmine.createSpy()
    } as unknown) as Event;
    component.onAddRouteClick(event, {
      id: '123',
      name: 'parser',
      type: 'lorem'
    });
    expect(event.preventDefault).toHaveBeenCalled();
    expect(spy).toHaveBeenCalledWith({
      id: '123',
      name: 'parser',
      type: 'lorem'
    });
  });

  it('should have a track function', () => {
    expect(component.trackByFn(0, '444')).toBe('444');
  });
});
