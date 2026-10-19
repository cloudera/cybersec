# Building from Source

Cybersec toolkit consists of a set of flink jobs packaged as jars, management scripts and a parcel file for installing on Cloudera Manager.

## Clone repo
```
git clone https://github.com/cloudera/cybersec.git
```

## Build with tests

```
cd cybersec/flink-cyber
mvn clean install
```

## Build without running tests
```
cd cybersec/flink-cyber
mvn clean install -DskipTests
```

## Location of cyber parcel and jobs
The build command creates release artifacts (.jar and parcel files) in the target directories of the maven subprojects.

| Release Artifact Source                                              | 
|----------------------------------------------------------------------|
| [Parcel](flink-cyber/cyber-parcel)                                   |
| [Parser Job](flink-cyber/parser-chains-flink)                        |
| [Parser Configuration UI](flink-cyber/metron-parser-chain/parser-chains-config-service)|
| [Triage Job](flink-cyber/flink-enrichment/flink-enrichment-combined) | 
| [Index Job](flink-cyber/flink-index/flink-index-hive)                |                
| [Profile Job](flink-cyber/flink-profiler-java)                       |
| [Scripts](flink-cyber/cyber-jobs/src/main/resources/scripts)         |

