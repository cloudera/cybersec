import {ParserChainModel} from "../../chain-page/chain-page.models";

export interface ClusterModel {
  jobConfigs?: { [key: string]: string },
  errorMessage?: { [key: string]: string };
  jobs: Job[],
  clusterMeta?: ClusterMeta,
}

export interface Job {
  jobIdString?: string,
  jobFullName?: string,
  jobName?: string,
  jobType?: string,
  jobBranch?: string,
  jobPipeline?: string,
  jobState?: string,
  startTime?: string
}

export interface ClusterMeta {
  name?: string,
  clusterId?: string,
  clusterStatus?: string,
  version?: string,
  flinkVersion?: string,
}

export interface RequestBody {
  clusterServiceId?: string,
  jobIdHex?: string,
  pipelineDir?: string,
  branch?: string,
  jobConfigs?: { [key: string]: string },
}
