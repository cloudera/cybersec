# Installation

## Choosing a Release

Pick the release that matches your cluster before installing. The
[release selection guide](release_readme.md) maps Cloudera versions and Cloudera Streaming Analytics
versions to the latest release that supports them.

## Packaging

The Cybersec Toolkit includes a Cloudera Manager parcel and service for easier installation.

Artifacts are available for download on the [releases page](https://github.com/cloudera/cybersec/releases).
You can also find less stable, but more up to date artifacts by selecting one of successful runs on [this page](https://github.com/cloudera/cybersec/actions/workflows/publish_release.yml) and scrolling to the bottom of the selected run page.

Or you can find artifacts after the build in the following directories:
1. [Parcel](flink-cyber/cyber-parcel)
2. [Cloudera Service](flink-cyber/cyber-csd)

## Building from Source

### Clone repo
```
git clone https://github.com/cloudera/cybersec.git
```

### Build with tests

```
cd cybersec/flink-cyber
mvn clean install
```

### Build without running tests
```
cd cybersec/flink-cyber
mvn clean install -DskipTests
```
