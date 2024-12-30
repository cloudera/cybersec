import {Component, ElementRef, inject, TemplateRef, ViewChild} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';
import {PipelineService} from 'src/app/services/pipeline.service';
import {filter, map, scan, shareReplay, startWith, switchMap} from 'rxjs/operators';
import {MatChipInputEvent} from '@angular/material/chips';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import {MatAutocomplete, MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {BehaviorSubject, combineLatest, merge, Observable} from 'rxjs';
import {ChainListPageService} from 'src/app/services/chain-list-page.service';
import {ChainModel} from 'src/app/chain-list-page/chain.model';
import {changeStateFn} from 'src/app/shared/utils';
import {MatDialog} from '@angular/material/dialog';
import {CustomChip} from 'src/app/shared/components/styled-chips-list/styled-chips-list.component';
import {PipelineSubmitState} from 'src/app/cluster/pipelines/pipeline-submit/pipeline-submit.component';
import {Router} from '@angular/router';


@Component({
  selector: 'app-pipeline-stepper',
  templateUrl: './pipeline-stepper.component.html',
  styleUrls: ['./pipeline-stepper.component.scss'],
})
export class PipelineStepperComponent {
  separatorKeysCodes: readonly number[] = [ENTER, COMMA];
  @ViewChild('sourceInput') sourceInput: ElementRef<HTMLInputElement>;
  @ViewChild('auto') autocomplete!: MatAutocomplete;

  private _formBuilder = inject(FormBuilder);
  private _pipelineService = inject(PipelineService);
  private _chainService = inject(ChainListPageService);
  // Chains Event triggers
  private _addChainSubject = new BehaviorSubject<string>(null);
  private _deleteChainByPipelineSubject = new BehaviorSubject<string>(null);
  private _deleteChainByIdSubject = new BehaviorSubject<string>(null);
  private _dialog = inject(MatDialog);
  private _router = inject(Router);

  // Chains Event
  private _addChainEvent$: Observable<(state: PipedChainModel[]) => PipedChainModel[]> = this._addChainSubject.pipe(
    filter(value => value !== null), // Filter out initial null emission
    switchMap(pipeline =>
      this._chainService.getChains(pipeline).pipe(
        map(ch => ch.map(c => ({...c, pipeline})))
      )
    ),
    map((chain: PipedChainModel[]) => (state: PipedChainModel[]) => [...state, ...chain]));
  private _deleteChainByPipelineEvent$ = this._deleteChainByPipelineSubject.pipe(
    filter(value => value !== null), // Filter out initial null emission
    map(pipeline => (state: PipedChainModel[]) => state.filter(pipedChain => pipedChain.pipeline !== pipeline)));
  private _deleteChainByIdEvent$ = this._deleteChainByIdSubject.pipe(
    filter(value => value !== null), // Filter out initial null emission
    map(id => changeStateFn(id, 'id')));


  private _currentChains$: Observable<PipedChainModel[]> = this._chainService.getChains().pipe(
    map(ch => ch.map(c => ({...c, pipeline: ''}))),
    switchMap(chains => {
      return merge(
        this._addChainEvent$,
        this._deleteChainByPipelineEvent$,
        this._deleteChainByIdEvent$
      ).pipe(
        startWith(lab => lab),
        scan((state, reducer) => reducer(state), chains),
      )
    }),
    shareReplay(1)
  );


  // constructor() {
  //   const navigation = this._router.getCurrentNavigation();
  //   this.state = navigation?.extras.state.data as PipelineSubmitState;
  // }

  state: PipelineSubmitState = this._router.getCurrentNavigation().extras.state.data as PipelineSubmitState;

  allPipeline$ = this._pipelineService.getPipelines().pipe(shareReplay());


  topicMap: Map<number, Map<string, CustomChip>> = new Map();


  parserFormGroup = this._formBuilder.group({
    topicOutput: [''],
    errorTopic: [''],
    sourceFlag: false,
    origBasePath: [''],
    storage: [null],
    chains: this._formBuilder.array(
      [this._formBuilder.group({
        chainName: 'Chain',
        source: '',
        selectedSource: this._formBuilder.control<string[]>([]),
        editMode: false,
      })]
    )
  });

  get vm$() {
    return combineLatest([
      this.allPipeline$,
      this._currentChains$
    ]).pipe(
      map(([pipelines, currentChains]) => ({pipelines, currentChains})),
      shareReplay()
    );
  }

  get chains() {
    return this.parserFormGroup.controls.chains; // Getter for the FormArray
  }

  getSelectedSource(i: number) {
    return this.chains.controls[i].controls.selectedSource.getRawValue()
  }


  addTab(): void {
    const defaultChain = this._formBuilder.group({
      chainName: `Chain ${this.chains.length}`,
      source: '',
      selectedSource: this._formBuilder.control<string[]>([]),
      editMode: false,
    });
    this.chains.push(defaultChain);

  }

  deleteChain(index: number): void {
    this.chains.removeAt(index)
  }

  dbClick(val: FormGroup) {
    val.setValue({...val.getRawValue(), editMode: !val.getRawValue().editMode})
  }

  add(event: MatChipInputEvent, index: number): void {
    const value = (event.value || '').trim();

    // Add our fruit
    if (value) {
      this._addChainSubject.next(value);
    }

    event.chipInput!.clear();
    const conf = this.chains.controls[index].getRawValue();
    const selectedSource = this.chains.controls[index].getRawValue().selectedSource;
    this.chains.controls[index].setValue({...conf, source: '', selectedSource: [...selectedSource, value]});
  }

  remove(value: string, index: number): void {
    const conf = this.chains.controls[index].getRawValue();
    const selectedSource = this.chains.controls[index].getRawValue().selectedSource.filter(v => v !== value);
    this.chains.controls[index].setValue({...conf, selectedSource});
    this._deleteChainByPipelineSubject.next(value);
  }

  selected(event: MatAutocompleteSelectedEvent, index: number) {
    const value = event.option.viewValue;
    this._addChainSubject.next(value);
    this.sourceInput.nativeElement.value = '';
    const conf = this.chains.controls[index].getRawValue();
    const selectedSource = this.chains.controls[index].getRawValue().selectedSource;
    this.chains.controls[index].setValue({...conf, source: '', selectedSource: [...selectedSource, value]});
  }

  deleteItem(id: string) {
    this._deleteChainByIdSubject.next(id);
  }

  assignTopic(ref: TemplateRef<any>, chainId: string, index: number) {
    this._dialog.open(ref, {
      minWidth: '30vw',
      data: {
        id: chainId,
        name: this.topicMap.get(index)?.get(chainId)?.name,
      }
    }).afterClosed().subscribe((data: { id: string, name: string }) => {
      if (data) {
        const tmp: Map<string, CustomChip> = new Map();
        tmp.set(chainId, {name: data.name, allowMapping: false, selected: true, removable: false});
        this.topicMap.set(index, tmp);
      }
    });
  }

  getSource(index: number): string {
    return this.chains.controls[index].controls.source.getRawValue();
  }

  navigateToReceiver() {
    this._currentChains$.subscribe(value => {
      const newState = {
        ...this.state,
        chainModel: value,
        chains: this.chains.getRawValue(),
      }
      this._router.navigate(['clusters/pipelines/submit'], {
        state: {data: newState}
      });
    });
  }
}

type PipedChainModel = ChainModel & { pipeline: string}
