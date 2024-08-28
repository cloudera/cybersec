import {Component, inject} from '@angular/core';
import {MultiButton} from 'src/app/shared/components/multibutton/multi-button.component';
import {AbstractControl, FormControl, FormGroup, Validators} from '@angular/forms';
import {ClusterService} from 'src/app/services/cluster.service';
import {combineLatest} from 'rxjs';
import {map} from 'rxjs/operators';
import {Router} from '@angular/router';
import {PipelineSubmitState} from 'src/app/cluster/pipelines/pipeline-submit/pipeline-submit.component';
import {ClusterMeta} from 'src/app/cluster/cluster-list-page/cluster-list-page.model';
import {JOBS_ENUM} from 'src/app/app.constants';

const BUTTONS_CONST = [
  {label: 'Archive', value: 'Archive', disabled: false},
  {label: 'Git', value: 'Git', disabled: false},
  {label: 'Empty', value: 'Empty', disabled: false},
  {label: 'Manual', value: 'Manual', disabled: true}
] as const;

const BUTTON_LABELS = BUTTONS_CONST.map(b => b.value);
type ButtonsKeys = keyof typeof BUTTONS_CONST
type ButtonsLabelValues = typeof BUTTON_LABELS[ButtonsKeys]
const urlRegex = /^(?:http(s)?:\/\/)?[\w.-]+(?:\.[\w\.-]+)+[\w\-\._~:/?#[\]@!\$&'\(\)\*\+,;=.]+$/;


@Component({
  selector: 'app-pipeline-create',
  templateUrl: './pipeline-create.component.html',
  styleUrls: ['./pipeline-create.component.scss']
})
export class PipelineCreateComponent {
  protected readonly jobsEnum = JOBS_ENUM;
  private readonly _maxSize = 1000000;
  private _router = inject(Router);
  private _clusterService = inject(ClusterService);

  jobs: string[] = Object.keys(JOBS_ENUM).map(key => JOBS_ENUM[key].value);
  mode: ButtonsLabelValues = 'Empty';

  buttons: Readonly<MultiButton[]> = BUTTONS_CONST;
  main = new FormGroup<{
    pipelineName: FormControl<string>,
    branchName: FormControl<string>,
    profileName: FormControl<string>,
    parserName: FormControl<string>,
    cluster: FormControl<ClusterMeta>
  }>({
    pipelineName: new FormControl('', [Validators.required, Validators.minLength(3)]),
    branchName: new FormControl('', [Validators.required, Validators.minLength(3)]),
    profileName: new FormControl('', [Validators.required, Validators.minLength(3)]),
    parserName: new FormControl('', [Validators.required, Validators.minLength(3)]),
    cluster: new FormControl(null, [Validators.required])
  });
  git = new FormGroup<{
    url: FormControl<string>,
    authentication: FormControl<boolean>,
    userName: FormControl<string>,
    password: FormControl<string>,
  }>({
      url: new FormControl(''),
      authentication: new FormControl(false),
      userName: new FormControl(''),
      password: new FormControl('')
    }
  );

  clusters$ = this._clusterService.getClusters();
  vm$ = combineLatest([this.clusters$]).pipe(map(([cluster]) => ({cluster})));
  fileControl: FormControl<File> = new FormControl(null);
  authentication = true;

  errorMessage(formControl: AbstractControl) {
    if (formControl.hasError('minlength')) {
      const minLengthError = formControl.errors.minlength;
      return `Minimum length required is ${minLengthError.requiredLength}, but actual length is ${minLengthError.actualLength}.`;
    }
    if (formControl.hasError('required')) {
      return 'Value is required.'
    }
    if (formControl.hasError('pattern')) {
      return 'Url format is incorrect.'
    }
    return 'Unrecognized error.';
  }

  selectPipelineMode(value: ButtonsLabelValues) {
    this.mode = value;
    switch (value) {
      case 'Archive':
        this.fileControl.setValidators(Validators.required);
        this.git.controls.url.clearValidators();
        break
      case 'Git':
        this.git.controls.url.setValidators([Validators.required, Validators.pattern(urlRegex)]);
        this.fileControl.clearValidators();
        break
      case 'Empty':
        this.git.clearValidators();
        this.fileControl.clearValidators();
        break
      case 'Manual':
      default:
    }
    this.fileControl.updateValueAndValidity();
    this.git.controls.url.updateValueAndValidity();
  }

  navigateToReceiver() {
    const data: PipelineSubmitState = {
      clusterId: this.main.controls.cluster.value.clusterId,
      pipelineName: this.main.controls.pipelineName.value,
      branch: this.main.controls.branchName.value,
      profileName: this.main.controls.profileName.value,
      parserName: this.main.controls.parserName.value,
      mode: this.mode.toString(),
      gitUrl: this.git.controls.url.value,
      userName: this.git.controls.userName.value,
      password: this.git.controls.password.value,
      file: this.fileControl.value,
      jobs: this.jobs,
    }
    this._router.navigate(['clusters/pipelines/submit'], {state: {data}});
  }

  onFileDropped(fileList: FileList) {
    this.prepareFilesList(fileList);
  }

  /**
   * handle file from browsing
   */
  fileBrowseHandler(event: Event) {
    const input = event.target as HTMLInputElement;
    this.prepareFilesList(input.files);
  }

  prepareFilesList(files: FileList) {
    Array.from(files).forEach((file) => {
      if (Math.ceil(file.size / 3) * 4 > this._maxSize) {
        alert(`'${file.name}' size is more than ${this.formatBytes(Math.ceil(this._maxSize / 4) * 3)}!`);
      } else {
        this.fileControl.setValue(file);
      }
    });
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) {
      return '0 Bytes';
    }
    const units = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return parseFloat((bytes / Math.pow(1024, i)).toFixed(2)) + ' ' + units[i];
  }

  toggleValueInArray<T>(array: T[], value: T): T[] {
    const index = array.indexOf(value);
    if (index !== -1) {
      // Value exists in the array, so remove it
      array.splice(index, 1);
    } else {
      // Value does not exist in the array, so add it
      array.push(value);
    }
    return array;
  }

  deleteFile() {
    this.fileControl.setValue(null);
  }
}
