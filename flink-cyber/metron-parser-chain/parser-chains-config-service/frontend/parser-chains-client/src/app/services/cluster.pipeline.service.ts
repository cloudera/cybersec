import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpRequest} from '@angular/common/http';
import {PipelineModel} from 'src/app/cluster/pipelines/pipeline.model';
import {RequestBody} from 'src/app/cluster/cluster-list-page/cluster-list-page.model';

@Injectable({
  providedIn: 'root'
})
export class ClusterPipelineService {

  static readonly BASE_URL = '/api/v1/clusters';
  private _http = inject(HttpClient);


  public createEmptyPipeline({clusterId, pipelineName, branch = 'main'}) {
    const body: RequestBody = {
      pipelineName,
      branch
    }
    return this._http.post(ClusterPipelineService.BASE_URL + `/${clusterId}/pipelines`, body);
  }

  public startPipelineArchive({clusterId, pipelineName, jobs, branch, profileName, parserName, file = null}) {
    const fd = new FormData();
    const blobJson = new Blob([JSON.stringify({
      jobs,
      pipelineName,
      profileName,
      parserName,
      branch,
    })], {
      type: 'application/json'
    });
    const blobFile = new Blob([file], {
      type: 'multipart/form-data'
    })
    fd.append('payload', blobFile);
    fd.append('body', blobJson);
    const req = new HttpRequest('POST', ClusterPipelineService.BASE_URL + `/${clusterId}/pipelines/${pipelineName}/archive`, fd);
    return this._http.request(req);
  }

  public startPipelineGit({clusterId, pipelineName, jobs, branch, profileName, parserName, gitUrl = '', userName ='', password =''}) {
    const body = {
      jobs,
      profileName,
      branch,
      pipelineName,
      parserName,
      gitUrl,
      userName,
      password
    }
    return this._http.post(ClusterPipelineService.BASE_URL + `/${clusterId}/pipelines/${pipelineName}/git`, body);
  }


  public getAllPipelines = () =>
    this._http.get<PipelineModel[]>(ClusterPipelineService.BASE_URL + '/pipelines');

  public getPipelines = (clusterId: string | number) =>
    this._http.get<PipelineModel[]>(`${ClusterPipelineService.BASE_URL}/${clusterId}/pipelines`);
}
