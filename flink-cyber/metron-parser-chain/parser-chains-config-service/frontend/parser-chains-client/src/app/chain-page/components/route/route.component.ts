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

import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {select, Store} from '@ngrx/store';
import {Subscription} from 'rxjs';

import * as fromActions from '../../chain-page.actions';
import {ParserChainModel, ParserModel, RouteModel} from '../../chain-page.models';
import {ChainPageState, getChain, getRoute} from '../../chain-page.reducers';

@Component({
  selector: 'app-route',
  templateUrl: './route.component.html',
  styleUrls: ['./route.component.scss']
})
export class RouteComponent implements OnInit, OnDestroy {

  @Input() routeId: string;
  @Input() parser: ParserModel;
  @Output() chainClick = new EventEmitter<string | ParserChainModel>();

  subchain: ParserChainModel;
  route: RouteModel;
  getRouteSub: Subscription;
  getChainSub: Subscription;

  constructor(
    private _store: Store<ChainPageState>,
  ) {
  }

  ngOnInit() {
    this.getRouteSub = this._store.pipe(select(getRoute({
      id: this.routeId
    }))).subscribe((route) => {
      this.route = route;
      if (route && route.subchain) {
        const id = typeof route.subchain === 'string' ? route.subchain : route.subchain.id;
        this.getChainSub = this._store.pipe(select(getChain({id})))
          .subscribe((subchain) => {
            this.subchain = subchain;
          });
      }
    });
  }

  onChainClick(event: Event, chainId: string | ParserChainModel) {
    this.chainClick.emit(chainId);
  }

  onMatchingValueBlur(event: Event, route: RouteModel) {
    const matchingValue = ((event.target as HTMLInputElement).value || '').trim();
    if (matchingValue !== route.matchingValue) {
      this._store.dispatch(
        new fromActions.UpdateChainAction({
          chain: {
            id: this.subchain.id,
            name: matchingValue
          }
        })
      );
      this._store.dispatch(
        new fromActions.UpdateRouteAction({
          chainId: this.subchain.id,
          parserId: this.parser.id,
          route: {
            id: route.id,
            matchingValue
          }
        })
      );
    }
  }

  onRouteRemoveConfirmed(event: Event, route: RouteModel) {
    this._store.dispatch(
      new fromActions.RemoveRouteAction({
        chainId: this.subchain.id,
        parserId: this.parser.id,
        routeId: route.id
      })
    );
  }

  onDefaultCheckboxChange(event: Event, route: RouteModel) {
    this._store.dispatch(
      new fromActions.SetRouteAsDefaultAction({
        chainId: this.subchain.id,
        parserId: this.parser.id,
        routeId: route.id
      })
    );
  }

  ngOnDestroy() {
    if (this.getChainSub) {
      this.getChainSub.unsubscribe();
    }
    if (this.getRouteSub) {
      this.getRouteSub.unsubscribe();
    }
  }
}
