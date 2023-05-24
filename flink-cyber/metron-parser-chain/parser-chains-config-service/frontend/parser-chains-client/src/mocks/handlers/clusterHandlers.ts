import {rest} from 'msw';
import clusterJson from "../../test/resources/parserconfig/clusters.json";
import {ClusterModel, Job} from "../../app/cluster/cluster-list-page/cluster-list-page.model";
import {ChainDetailsModel, ParserModel} from "../../app/chain-page/chain-page.models";


const cluster = clusterJson as ClusterModel[];

if (localStorage.getItem('cluster') === null) {
  localStorage.setItem('cluster', JSON.stringify(cluster));
}

export const clusterHandlers = [
  rest.get<ClusterModel[]>('/api/v1/clusters', (req, res, ctx) => {
    const storedCluster: ClusterModel[] = JSON.parse(localStorage.getItem('cluster'));
    return res(
      ctx.status(200),
      ctx.json(storedCluster)
    );
  }),
  rest.get<ClusterModel, { clusterId: string }>('/api/v1/clusters/:clusterId', (req, res, ctx) => {
    const {clusterId} = req.params;
    const storedChains: ClusterModel[] = JSON.parse(localStorage.getItem('cluster'));
    const chain = storedChains.find(chain => chain.id === clusterId);
    if (chain) {
      return res(
        ctx.status(200),
        ctx.delay(1000),
        ctx.json(chain)
      );
    } else {
      return res(
        ctx.status(404)
      )
    }
  }),
  rest.patch<any, { clusterId: string, jobName: string }>('/api/v1/clusters/:clusterId/jobs/:jobName', (req, res, ctx) => {
    const {clusterId, jobName} = req.params;
    const reqBody: Map<string,string> = req.body;

    const storedClusters: ClusterModel[] = JSON.parse(localStorage.getItem('cluster'));
    const clusterModel = storedClusters.find(chain => chain.id === clusterId);
    const job: Job = clusterModel?.jobs.find(job => job.name === jobName);
    if (jobName.toLowerCase().includes('triage')) {
      return res(
        ctx.status(400)
      );
    }
    if (!job) {
      return res(
        ctx.status(404)
      );
    }

    Object.entries(reqBody).forEach(([key, value]) => {
      if (job.hasOwnProperty(key)) {
        job[key] = value;
      } else {
        return res(
          ctx.status(415)
        );
      }
    });

    localStorage.setItem('cluster', JSON.stringify(storedClusters));
    return res(
      ctx.status(204),
    );
  }),

];
