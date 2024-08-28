import {Injectable} from '@angular/core';
import {HttpClient, HttpResponse} from '@angular/common/http';
import {ClusterModel, RequestBody} from '../cluster/cluster-list-page/cluster-list-page.model';

@Injectable({
  providedIn: 'root'
})
export class ClusterService {

  static readonly BASE_URL = '/api/v1/';

  constructor(
    private _http: HttpClient
  ) {
  }

  public getClusters = () =>
    this._http.get<ClusterModel[]>(ClusterService.BASE_URL + 'clusters');

  public getCluster = (clusterId: string | number) =>
    this._http.get<ClusterModel>(`${ClusterService.BASE_URL}clusters/${clusterId}`);

  public sendJobCommand = (clusterId: string | number, action: string, requestBody: RequestBody) =>
    this._http.post<HttpResponse<any>>(`${ClusterService.BASE_URL}clusters/${clusterId}/jobs/${action}`, requestBody);

  public uploadFile = (clusterId: string | number, pipeline: string, jobIdHex: string, file: any) =>
    this._http.post<HttpResponse<any>>(`${ClusterService.BASE_URL}clusters/${clusterId}/jobs/config/${pipeline}/${jobIdHex}`, file);
}
