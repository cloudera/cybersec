import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, NavigationStart, Router } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { NzModalService } from 'ng-zorro-antd';
import { Observable, Observer, Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

import { DeactivatePreventer } from '../misc/deactivate-preventer.interface';

import * as fromActions from './chain-page.actions';

import { ChainDetailsModel, ParserChainModel } from './chain-page.models';
import {
  ChainPageState,
  getChain,
  getChainDetails,
  getDirtyStatus,
  getParserToBeInvestigated,
  getPathWithChains
} from './chain-page.reducers';
import { getFailedParser } from './components/live-view/live-view.reducers';

@Component({
  selector: 'app-chain-page',
  templateUrl: './chain-page.component.html',
  styleUrls: ['./chain-page.component.scss']
})
export class ChainPageComponent implements OnInit, OnDestroy, DeactivatePreventer {

  chain: ParserChainModel;
  breadcrumbs: ParserChainModel[] = [];
  chainId: string;
  dirtyChains: string[] = [];
  dirtyParsers: string[] = [];
  chainConfig$: Observable<ChainDetailsModel>;
  parserToBeInvestigated: string[] = [];
  getChainSubscription: Subscription;
  forceDeactivate = false;
  chainIdBeingEdited: string;
  getChainsSubscription: Subscription;
  popOverVisible = false;
  @ViewChild('chainNameInput', { static: false }) chainNameInput: ElementRef;
  editChainNameForm: FormGroup;
  failedParser$: Observable<string>;

  constructor(
    private store: Store<ChainPageState>,
    private activatedRoute: ActivatedRoute,
    private modal: NzModalService,
    private router: Router,
    private fb: FormBuilder
  ) { }

  get dirty() {
    return this.dirtyParsers.length || this.dirtyChains.length;
  }

  get parsers() {
    if (this.parserToBeInvestigated.length) {
      return this.parserToBeInvestigated;
    } else {
      return (
        (this.breadcrumbs.length > 0 &&
          this.breadcrumbs[this.breadcrumbs.length - 1].parsers) ||
        (this.chain && this.chain.parsers)
      );
    }
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      this.chainId = params.id;
    });

    this.getChainsSubscription = this.store.pipe(select(getPathWithChains)).subscribe((path) => {
      this.breadcrumbs = path;
    });

    this.getChainSubscription = this.store.pipe(select(getChain, { id: this.chainId })).subscribe((chain: ParserChainModel) => {
      if (!chain) {
        this.store.dispatch(new fromActions.LoadChainDetailsAction({
          id: this.chainId
        }));
      } else {
        this.chain = chain;
      }
    });

    this.store.pipe(select(getDirtyStatus)).subscribe((status) => {
      this.dirtyParsers = status.dirtyParsers;
      this.dirtyChains = status.dirtyChains;
    });

    this.chainConfig$ = this.store.pipe(select(getChainDetails, { chainId: this.chainId }));

    this.store.pipe(select(getParserToBeInvestigated)).subscribe((id: string) => {
      this.parserToBeInvestigated = id === '' ? [] : [id];
    });

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        const regex = new RegExp('^\/parserconfig\/chains\/' + this.chainId + '\/new', 'gi');
        if (event.url && event.url.match(regex)) {
          this.forceDeactivate = true;
        }
      }
    });

    this.editChainNameForm = this.fb.group({
      name: new FormControl(null, [Validators.required, Validators.minLength(3)])
    });

    this.store.dispatch(new fromActions.GetFormConfigsAction());

    this.failedParser$ = this.store.pipe(select(getFailedParser));
  }

  exitFailedParserEditView() {
    this.store.dispatch(new fromActions.InvestigateParserAction({ id: '' }));
  }

  removeParser(id: string) {
    const chainId = this.breadcrumbs.length > 0
      ? this.breadcrumbs[this.breadcrumbs.length - 1].id
      : this.chainId;
    this.store.dispatch(new fromActions.RemoveParserAction({
      id,
      chainId
    }));
  }

  onChainLevelChange(chainId: string) {
    this.store.pipe(select(getChain, { id: chainId })).pipe(take(1)).subscribe((chain: ParserChainModel) => {
      this.store.dispatch(
        new fromActions.AddToPathAction({ chainId })
      );
    });
  }

  onBreadcrumbClick(event: Event, chain: ParserChainModel) {
    event.preventDefault();
    const index = this.breadcrumbs.findIndex((breadcrumb: ParserChainModel) => breadcrumb.id === chain.id);
    const removeFromPathList = this.breadcrumbs.slice(index + 1).map(ch => ch.id);
    if (removeFromPathList.length) {
      this.store.dispatch(
        new fromActions.RemoveFromPathAction({ chainId: removeFromPathList })
      );
    }
  }

  onChainNameEditClick(event: Event, chain: ParserChainModel) {
    event.preventDefault();
    this.editChainNameForm.get('name').setValue(chain.name);
  }

  onChainNameEditDone(chain: ParserChainModel) {
    this.popOverVisible = false;
    const value = (this.editChainNameForm.get('name').value || '').trim();
    if (value !== chain.name) {
      this.store.dispatch(new fromActions.UpdateChainAction({
        chain: {
          id: chain.id,
          name: value
        }
      }));
    }
  }

  onChainNameEditCancel() {
    this.popOverVisible = false;
  }

  canDeactivate(): Observable<boolean> {
    const allow = (o: Observer<boolean>) => { o.next(true); o.complete(); };
    const deny  = (o: Observer<boolean>) => { o.next(false); o.complete(); };

    return new Observable((observer: Observer<boolean>) => {
      if (this.dirty && !this.forceDeactivate) {
        this.modal.confirm({
          nzTitle: 'You have unsaved changes',
          nzContent: 'Are you sure you want to leave this page?',
          nzOkText: 'Leave',
          nzOkType: 'danger',
          nzCancelText: 'Cancel',
          nzOnOk: () => allow(observer),
          nzOnCancel: () => deny(observer),
        });
      } else {
        allow(observer);
        this.forceDeactivate = false;
      }
    });
  }

  onResetChanges() {
    this.modal.confirm({
      nzTitle: 'Your changes will be lost',
      nzContent: 'Are you sure you want to reset?',
      nzOkText: 'Reset',
      nzOkType: 'danger',
      nzCancelText: 'Cancel',
      nzOnOk: () => {
        this.store.dispatch(new fromActions.LoadChainDetailsAction({
          id: this.chainId
        }));
        this.store.dispatch(new fromActions.InvestigateParserAction({ id: '' }));
      }
    });
  }

  onSaveChanges() {
    this.modal.confirm({
      nzTitle: 'You are about the save your changes',
      nzContent: 'Are you sure you want to save?',
      nzOkText: 'Save',
      nzOkType: 'primary',
      nzCancelText: 'Cancel',
      nzOnOk: () => {
        this.store.dispatch(new fromActions.SaveParserConfigAction({ chainId: this.chainId }));
        this.store.dispatch(new fromActions.InvestigateParserAction({ id: '' }));
      }
    });
  }

  onAddParserClick(event: Event) {
    event.preventDefault();
    const routeParams = this.breadcrumbs.length > 1
      ? { subchain: this.breadcrumbs[this.breadcrumbs.length - 1].id }
      : {};
    this.router.navigate([`/parserconfig/chains/${this.chainId}/new`, routeParams]);
  }

  ngOnDestroy() {
    this.breadcrumbs = [];
    if (this.getChainsSubscription) {
      this.getChainsSubscription.unsubscribe();
    }
    if (this.getChainSubscription) {
      this.getChainSubscription.unsubscribe();
    }
  }
}
