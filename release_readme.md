# Choosing a Release

The Cybersec Toolkit is built against a specific Cloudera Streaming Analytics (CSA) version and a
specific Cloudera runtime version. Use the table below to pick the release that matches the software
running in your cluster.

Find your Cloudera version in the first column, read across to confirm the Cloudera Streaming
Analytics version, then follow the link in the last column to download the newest release that
supports that combination.

| Cloudera Version | Cloudera Streaming Analytics Version | Latest Release |
| --- | --- | --- |
| CDP 7.3.2 | 1.17.0.0 | [csa-1.17.0.0-cdh-7.3.2-jdk-17-release-09](https://github.com/cloudera/cybersec/releases/tag/csa-1.17.0.0-cdh-7.3.2-jdk-17-release-09) |
| CDP 7.3.1 | 1.16.1.0 | [release-csa-1.16.1.0-7.3.1-backport-01](https://github.com/cloudera/cybersec/releases/tag/release-csa-1.16.1.0-7.3.1-backport-01) |
| CDP 7.3.1.500 | 1.16.0.0 | [csa-1.16.0.0-7.3.1.500-02](https://github.com/cloudera/cybersec/releases/tag/csa-1.16.0.0-7.3.1.500-02) |
| Data Hub 7.3.1 | 1.15.2.0 | [csa-1.15.2.0-dh-7.3.1-004](https://github.com/cloudera/cybersec/releases/tag/csa-1.15.2.0-dh-7.3.1-004) |
| CDP 7.3.1 | 1.14.1.0 | [release_csa_1.14.1.0_cdp_7.3.1_01](https://github.com/cloudera/cybersec/releases/tag/release_csa_1.14.1.0_cdp_7.3.1_01) |
| Data Hub 7.3.1 | 1.14.0.0 | [csa-1.14.0.0-dh-7.3.1-002](https://github.com/cloudera/cybersec/releases/tag/csa-1.14.0.0-dh-7.3.1-002) |
| CDP 7.1.9 | 1.12.0 | [csa-1.12.0-cdh-7.1.9-002](https://github.com/cloudera/cybersec/releases/tag/csa-1.12.0-cdh-7.1.9-002) |
| Data Hub 7.2.18 | 1.12.0 | [csa-1.12.0-dh-7.2.18-001](https://github.com/cloudera/cybersec/releases/tag/csa-1.12.0-dh-7.2.18-001) |

## Notes

* The Cloudera Streaming Analytics version in a release is also encoded in the release tag, for
  example `csa-1.17.0.0-cdh-7.3.2-jdk-17-release-09` targets CSA 1.17.0.0 on CDH 7.3.2 with JDK 17.
* Newer releases are published over time. Browse the full
  [releases page](https://github.com/cloudera/cybersec/releases) if you need a specific build that
  is not listed here.
* Each row points to the most recent release for that combination. Earlier builds are still available
  from the [releases page](https://github.com/cloudera/cybersec/releases).
