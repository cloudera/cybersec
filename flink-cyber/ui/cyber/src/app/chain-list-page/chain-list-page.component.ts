import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { LoadChainsAction } from './chain-list-page.actions';
import * as fromActions from './chain-list-page.actions';
import { ChainListPageState, getChains } from './chain-list-page.reducers';
import { ChainModel, ChainOperationalModel } from './chain.model';

@Component({
  selector: 'app-chain-list-page',
  templateUrl: './chain-list-page.component.html',
  styleUrls: ['./chain-list-page.component.scss']
})
export class ChainListPageComponent implements OnInit {
  isChainModalVisible = false;
  isOkLoading = false;
  chains$: Observable<ChainModel[]>;
  totalRecords = 200;
  chainDataSorted$: Observable<ChainModel[]>;
  sortDescription$: BehaviorSubject<{key: string, value: string}> = new BehaviorSubject({ key: 'name', value: '' });
  newChainForm: FormGroup;

  constructor(
    private store: Store<ChainListPageState>,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    ) {
    this.route.queryParams.subscribe(() => {
      store.dispatch(new LoadChainsAction());
      this.chains$ = store.pipe(select(getChains));
    });

    this.chainDataSorted$ = combineLatest([
      this.chains$,
      this.sortDescription$
    ]).pipe(
      switchMap(([ chains, sortDescription ]) => this.sortTable(chains, sortDescription))
    );
  }

  get chainName() {
    return this.newChainForm.get('chainName') as FormControl;
  }

  showAddChainModal(): void {
    this.isChainModalVisible = true;
  }

  pushChain(): void {
    const chainData: ChainOperationalModel = { name: this.chainName.value };
    this.store.dispatch(new fromActions.CreateChainAction(chainData));
    this.isChainModalVisible = false;
  }

  deleteChain(chainId: string): void {
    this.store.dispatch(new fromActions.DeleteChainAction(chainId));
  }
  handleCancel(): void {
    this.isChainModalVisible = false;
  }

  sortTable(data: ChainModel[], sortDescription: any): Observable<ChainModel[]> {
    const sortValue = sortDescription.value;
    const newData = (data || []).slice().sort((a, b) => {
      const first = a.name.toLowerCase();
      const second = b.name.toLowerCase();
      if (sortValue === 'ascend') {
        return (first < second) ? -1 : 1;
      } else if (sortValue === 'descend') {
        return (first < second) ? 1 : -1;
      } else {
        return 0;
      }
    });
    return of(newData);
  }

  ngOnInit() {
    this.newChainForm = this.fb.group({
      chainName: new FormControl('', [Validators.required, Validators.minLength(3)]),
    });
  }
}
