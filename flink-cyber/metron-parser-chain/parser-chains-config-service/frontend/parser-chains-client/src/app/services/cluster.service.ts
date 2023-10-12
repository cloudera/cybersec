import {Injectable} from "@angular/core";
import {HttpClient, HttpResponse} from "@angular/common/http";
import {ClusterModel, RequestBody} from "../cluster/cluster-list-page/cluster-list-page.model";

@Injectable({
  providedIn: 'root'
})
export class ClusterService {

  private readonly BASE_URL = '/api/v1/';

  constructor(
    private http: HttpClient
  ) {}

  public getClusters = () =>
    this.http.get<ClusterModel[]>(this.BASE_URL + 'clusters');

  public getCluster = (clusterId : string |  number) =>
    this.http.get<ClusterModel>(`${this.BASE_URL}clusters/${clusterId}`);

  public sendJobCommand = (clusterId : string | number, action : string , requestBody: RequestBody) =>
    this.http.post<HttpResponse<any>>(`${this.BASE_URL}clusters/${clusterId}/jobs/${action}`, requestBody);
}
