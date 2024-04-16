import {rest} from 'msw';
import clusterJson from "../../test/resources/parserconfig/clusters.json";


const cluster = clusterJson;

if (localStorage.getItem('cluster') === null) {
  localStorage.setItem('cluster', JSON.stringify(cluster));
}

export const clusterHandlers = [
  rest.get('/api/v1/clusters', (req, res, ctx) => {
    const storedCluster = JSON.parse(localStorage.getItem('cluster'));
    return res(
      ctx.status(200),
      ctx.json(storedCluster)
    );
  }),
  rest.get<any, { clusterId: string }>('/api/v1/clusters/:clusterId', (req, res, ctx) => {
    const {clusterId} = req.params;
    const storedChains = JSON.parse(localStorage.getItem('cluster'));
    const chain = storedChains.find(ch => ch.clusterMeta.clusterId === clusterId);
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
  rest.post<any, { clusterId: string }>('/api/v1/clusters/:clusterId/:action', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.delay(100),
    );
  }),
  rest.post<any, { clusterId: string }>('/api/v1/clusters/:clusterId/config/:pipeline/:jobIdHex', (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.delay(100),
    );
  }),
];
