import {Component, inject, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {ClusterPipelineService} from 'src/app/services/cluster.pipeline.service';

export type PipelineSubmitState = {
  clusterId: string;
  pipelineName: string;
  branch: string;
  profileName: string;
  parserName: string;
  file?: File;
  jobs: string[]
  gitUrl?: string,
  userName?: string,
  password?: string,
  mode?: string,
}

@Component({
  selector: 'app-pipeline-submit',
  templateUrl: './pipeline-submit.component.html',
  styleUrls: ['./pipeline-submit.component.scss']
})
export class PipelineSubmitComponent implements OnInit {
  private _route = inject(Router);
  private _clusterPipelineService = inject(ClusterPipelineService);
  loadingEmpty = true;
  loadingStartAllJob = false;
  state: PipelineSubmitState;
  finishedSubmitAllJob = false;

  constructor() {
    const navigation = this._route.getCurrentNavigation();
    this.state = navigation?.extras.state.data as PipelineSubmitState;
  }

  ngOnInit(): void {
    this._clusterPipelineService.createEmptyPipeline(this.state).subscribe(() => {
      this.loadingEmpty = false;
      this.loadingStartAllJob = true;
      switch (this.state.mode) {
        case "Archive":
          this.loadingStartAllJob = true;
          this._clusterPipelineService.startPipelineArchive(this.state).subscribe(
            () => {
              this.loadingStartAllJob = false;
              this.finishedSubmitAllJob = true;
            }
        )
          break;
        case "Git":
          this.loadingStartAllJob = true;
          this._clusterPipelineService.startPipelineGit(this.state).subscribe(
            () => {
              this.loadingStartAllJob = false;
              this.finishedSubmitAllJob = true;
            }
          )

      }
    })
  }

}
