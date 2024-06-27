import {Component, inject} from '@angular/core';
import {MultiButton} from 'src/app/shared/components/multibutton/multi-button.component';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ClusterService} from 'src/app/services/cluster.service';
import {combineLatest} from 'rxjs';
import {map} from 'rxjs/operators';
import {Router} from '@angular/router';
import {
  PipelineSubmitState
} from 'src/app/cluster/pipelines/pipeline-submit/pipeline-submit.component';
import {ClusterMeta} from 'src/app/cluster/cluster-list-page/cluster-list-page.model';
import {JOBS_ENUM} from 'src/app/app.constants';

const BUTTONS_CONST = [
  {label: 'Archive', value: 'Archive', disabled: false},
  {label: 'Git', value: 'Git', disabled: true},
  {label: 'Empty', value: 'Empty', disabled: false},
  {label: 'Manual', value: 'Manual', disabled: true}
] as const;

const BUTTON_LABELS = BUTTONS_CONST.map(b => b.value);
type ButtonsKeys = keyof typeof BUTTONS_CONST
type ButtonsLabelValues = typeof BUTTON_LABELS[ButtonsKeys]


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

  jobs: string[] = Object.keys(JOBS_ENUM).map(key=> JOBS_ENUM[key].value);
  mode: ButtonsLabelValues = 'Empty';
  file: File;

  buttons: Readonly<MultiButton[]> = BUTTONS_CONST;
  formGroup = new FormGroup<{
    pipelineName: FormControl<string>,
    branchName: FormControl<string>,
    profileName: FormControl<string>,
    cluster: FormControl<ClusterMeta>
  }>({
    pipelineName: new FormControl('', [Validators.minLength(3)]),
    branchName: new FormControl('', [Validators.minLength(3)]),
    profileName: new FormControl('', [Validators.minLength(3)]),
    cluster: new FormControl({})
  });
  clusters$ = this._clusterService.getClusters();
  vm$ = combineLatest([this.clusters$]).pipe(map(([cluster]) => ({cluster})))

  selectPipelineMode(value: ButtonsLabelValues) {
    this.mode = value;
  }

  navigateToReceiver() {
    const data: PipelineSubmitState = {
      clusterId: this.formGroup.controls.cluster.getRawValue().clusterId,
      pipelineName: this.formGroup.controls.pipelineName.getRawValue(),
      branch: this.formGroup.controls.branchName.getRawValue(),
      profileName: this.formGroup.controls.branchName.getRawValue(),
      showStartAllJob: this.mode === "Archive",
      jobs: this.jobs,
      file: this.file,
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
        this.file = file;
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
}
