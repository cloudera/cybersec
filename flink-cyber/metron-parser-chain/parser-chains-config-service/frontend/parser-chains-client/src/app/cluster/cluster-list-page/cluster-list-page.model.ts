export interface ClusterModel {
  id: string,
  status: string,
  name: string,
  version: string,
  jobs: Job[],
}

export interface Job {
  name?: string,
  branch?: string,
  pipeline?: string,
  status?: string,
  created?: string
}
