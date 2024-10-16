import {Component, inject, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {ClusterPipelineService} from 'src/app/services/cluster.pipeline.service';

export type PipelineSubmitState = {
  clusterId: string;
  pipelineName: string;
  branch: string;
  showStartAllJob?: boolean;
  profileName?: string;
  file?: any;
  jobs?: string[]
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
    this._clusterPipelineService.createEmptyPipeline(this.state.clusterId, this.state.pipelineName, this.state.branch).subscribe(() => {
      this.loadingEmpty = false;
      this.loadingStartAllJob = true;
      if (this.state.showStartAllJob) {
        this.loadingStartAllJob = true;
        this._clusterPipelineService.startAllPipelines(this.state.clusterId, this.state.pipelineName, this.state.jobs, this.state.branch, this.state.profileName, this.state.file).subscribe(
          () => {
            this.loadingStartAllJob = false;
            this.finishedSubmitAllJob = true;
          }
        )
      }
    })
  }

}
