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

import {Component, inject} from '@angular/core';
import {BehaviorSubject, combineLatest, merge, Observable, of, Subject} from 'rxjs';
import {catchError, filter, map, scan, shareReplay, startWith, switchMap} from 'rxjs/operators';
import {ChainModel, ChainOperationalModel, DialogData, DialogForm} from './chain.model';
import {PipelineService} from '../services/pipeline.service';
import {ChainListPageService} from '../services/chain-list-page.service';
import {MatDialog, MatDialogConfig} from '@angular/material/dialog';
import {ChainDialogComponent} from 'src/app/chain-list-page/component/chain-dialog.component';
import {
  ConfirmDeleteDialogComponent,
  DeleteDialogData
} from 'src/app/shared/components/confirm-delete-dialog/confirm-delete-dialog.component';
import {changeStateFn} from 'src/app/shared/utils';

@Component({
  selector: 'app-chain-list-page',
  templateUrl: './chain-list-page.component.html',
  styleUrls: ['./chain-list-page.component.scss'],
})
export class ChainListPageComponent {
  //Injects and services
  private _pipelineService = inject(PipelineService);
  private _chainService = inject(ChainListPageService);
  private _dialog = inject(MatDialog);

  // Pipelines
  private _currentPipelineSubject = new BehaviorSubject<string>('');
  private _currentPipeline$ = this._currentPipelineSubject.asObservable();
  private _pipelinesSubject = new Subject<string[]>();

  // Chains Event triggers
  private _addChainSubject = new BehaviorSubject<ChainModel>(null);
  private _deleteChainSubject = new BehaviorSubject<string>(null);

  // Chains Event
  private _addChainEvent$ = this._addChainSubject.pipe(
    filter(value => value !== null), // Filter out initial null emission
    map(chain => (state: ChainModel[]) => [...state, chain]));
  private _deleteChainEvent$ = this._deleteChainSubject.pipe(
    filter(value => value !== null), // Filter out initial null emission
    map(id => changeStateFn<ChainModel>(id, 'id')));

  // fetched Chains of currentPipeline
  private _currentChains$ = this._currentPipeline$.pipe(
    switchMap(pipeline => this._chainService.getChains(pipeline)),
    switchMap(chains => {
      return merge(
        this._addChainEvent$,
        this._deleteChainEvent$
      ).pipe(
        startWith(lab => lab),
        scan((state, reducer) => reducer(state), chains),
      )
    }),
    shareReplay(1)
  );
  pipelines = merge(this._pipelinesSubject,
    this._pipelineService.getPipelines()
      .pipe(
      catchError((err, thr) => {
        return of([]);
      }))
  ).pipe(
    shareReplay(1)
  );

  // Combined data
  get vm$() {
    return combineLatest([
      this._currentPipeline$,
      this.pipelines,
      this._currentChains$
    ]).pipe(
      map(([currentPipeline, pipelines, currentChains]) => ({currentPipeline, pipelines, currentChains}))
    );
  }

  openCreateDialog(currentPipeline: string) {
    const createChainPipeline = (form: DialogForm): Observable<ChainModel> => this._chainService.createChain(form as ChainOperationalModel, currentPipeline);
    const config: MatDialogConfig<DialogData<ChainModel>> = {
      ...dialogSize,
      data: {
        type: 'create',
        name: 'Chain',
        currentValue: '',
        action: createChainPipeline.bind(this._chainService),
        existValues: this._currentChains$,
        columnUniqueKey: 'name'
      },
    };
    this._dialog.open(ChainDialogComponent, config).afterClosed().subscribe((result: {
      response: ChainModel,
      form: DialogForm
    }) => {
      if (result) {
        this._addChainSubject.next(result.response);
      }
    });
  }

  openChainDeleteDialog(chainDelete: ChainModel, currentPipeline: string) {
    const deleteChain = () => this._chainService.deleteChain(chainDelete.id, currentPipeline);
    const config: MatDialogConfig<DeleteDialogData> = {
      ...dialogSize,
      data: {
        action: deleteChain.bind(this._chainService)
      },
    };
    this._dialog.open(ConfirmDeleteDialogComponent, config).afterClosed().subscribe(_ => {
      this._deleteChainSubject.next(chainDelete.id);
    });
  }

  addPipeline(pipelines: string[]) {
    const addPipeline = (form: DialogForm) => this._pipelineService.createPipeline(form.name);
    const config: MatDialogConfig<DialogData<string>> = {
      ...dialogSize,
      data: {
        type: 'create',
        name: 'Pipeline',
        action: addPipeline.bind(this._pipelineService),
        existValues: pipelines
      },
    };
    this._dialog.open(ChainDialogComponent, config).afterClosed().subscribe((result: {
      response: string[],
      form: DialogForm
    }) => {
      if (result) {
        this._pipelinesSubject.next(result.response);
      }
    });
  }

  updatePipeline(pipelines: string[], currentPipeline: string) {
    const renamePipeline = (form: DialogForm) => this._pipelineService.renamePipeline(form.name, currentPipeline);
    const config: MatDialogConfig<DialogData<string>> = {
      ...dialogSize,
      data: {
        type: 'edit',
        name: 'Pipeline',
        currentValue: currentPipeline,
        action: renamePipeline.bind(this._pipelineService),
        existValues: pipelines
      },
    };
    this._dialog.open(ChainDialogComponent, config).afterClosed().subscribe((result: {
      response: string[],
      form: DialogForm
    }) => {
      if (result) {
        this._pipelinesSubject.next(result.response);
        this._currentPipelineSubject.next(result.form.name);
      }
    });

  }

  deletePipeline(currentPipeline: string) {
    const deletePipeline = () => this._pipelineService.deletePipeline(currentPipeline);
    const config: MatDialogConfig<DeleteDialogData> = {
      ...dialogSize,
      data: {
        action: deletePipeline.bind(this._pipelineService)
      },
    };
    this._dialog.open(ConfirmDeleteDialogComponent, config).afterClosed().subscribe((result) => {
      this._pipelinesSubject.next(result);
      this._currentPipelineSubject.next('');

    });
  }

  onPipelineSelected(value: string) {
    this._currentPipelineSubject.next(value);
  }
}

const dialogSize = {
  width: '30vw',
  minWidth: '350px',
  maxWidth: '80vw'
};
