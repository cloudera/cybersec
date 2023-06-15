# Cybersec Toolkit

## Overview
Enterprises deploy many point solutions to defend their networks.  These point solutions provide a wealth of data about the enterprise assets and networks but it is difficult to provide analytics on this data because there is no common repository and the events are in different formats.  The Cybersec Toolkit is a pipeline that ingests, correlates and prepares cybersecurity data for analytics.  The Cyber Toolkit leverages the Cloudera Data Platform to build a Security Data Lakehouse.

The Cyber Toolkit ingests raw log events from a variety of sources, parses and normalizes the log events using a common schema, enriches the events with reference data, scores the log events, profiles the events, and streams the events to a Kafka and a data lakehouse.   Integrate with orchestration or investigation and ticketing platforms using Flink SQL (SQL Stream Builder) on the triaged event topic.  Query the data lakehouse using SQL for visualizations and ad hoc queries or Spark for notebooks, investigations and machine learning model training.

The Cyber Toolkit is flexible and configurable so the ingestion can be changed with low or no code.
 
## Ingestion Stages
1. [Parse](flink-cyber/parser-chains-flink/README.md)
2. [Triage](flink-cyber/flink-enrichment/flink-enrichment-combined/README.md)
3. [Index](flink-cyber/flink-indexing/flink-indexing-hive/README.md)
4. [Profile](flink-cyber/flink-profiler-java/README.md)

## Tools
1. [Batch Enrichment Load](flink-cyber/flink-enrichment/flink-enrichment-load/README.md)
2. [Upsert Scoring Command](flink-cyber/flink-commands/scoring-commands/README.md)
3. [Event Generation](flink-cyber/caracal-generator/README.md)

## Packaging
The Cybersec Toolkit includes a Cloudera Manager parcel and service for easier installation. 
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

