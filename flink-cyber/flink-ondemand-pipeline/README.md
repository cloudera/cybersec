# On-Demand Enrichment Pipeline

The on-demand enrichment pipeline re-enriches events that were already indexed, typically after
new enrichment data or scoring rules have been loaded.  The regular [triaging
job](../flink-enrichment/flink-enrichment-combined/README.md) only enriches events as they arrive
from the parsers.  When reference data changes, the events that were indexed before the change are
stale.  The on-demand pipeline reads those events back out of the index, runs the same enrichment
and scoring steps over them, and writes the enriched results to a separate destination database.

Typical uses:

* Re-score historical events after loading new scoring rules.
* Enrich historical events after loading new reference data (threat intel, geolocation, lookups, ...).
* Backfill a newly added enrichment field on events that were indexed before the field existed.

## How it works

The job reads already-indexed events for a time range and a set of sources, runs them through the
triaging enrichment pipeline, and writes the enriched results to a destination database.

1. **Read** - For each source listed in `sources`, the job generates a `SELECT` against the
   *unenriched* index table that holds the source.  The query filters on the `dt`/`hr` partition
   columns and the `ts` timestamp column for the configured time range, and on the `source` column.
   The score columns and the partition columns are not read back.
2. **Convert** - Each row is converted back into a `Message` using the unenriched column mappings
   from the index mapping file.  Rows that cannot be converted are written to the error report file
   instead of failing the job.
3. **Enrich and score** - The messages run through the same enrichment and scoring pipeline as the
   triaging job (geolocation, ASN, cidr, local lookups, HBase, rest, ThreatQ, Stellar, scoring).
4. **Write** - The scored messages are written to the primary index tables in the destination
   database.  Only the primary tables for the configured sources are written; the derived unenriched
   tables are left to the regular indexing job.

The read, enrichment and write steps run as a single Flink job.

The source tables must be Iceberg tables in the Hive metastore.  Only the `iceberg` output connector
is supported.

## Configuration

The on-demand job does not hold the enrichment or indexing settings itself.  Instead it refers to a
triaging configuration file and an indexing configuration file by path, and adds the settings that
describe the historical run.

### On-Demand Properties

| Property Name           | Type                 | Description                                                                                                                                                                                          | Required/Default | Example                                |
|-------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|----------------------------------------|
| triage.config           | File path            | Path to the [triaging job properties file](../flink-enrichment/flink-enrichment-combined/README.md).  Supplies the enrichment and scoring settings.                                                  | Required         | ondemand-triage.properties             |
| index.config            | File path            | Path to the [indexing job properties file](../flink-indexing/flink-indexing-hive/README.md).  Supplies the source catalog, database, tables and mappings.  Must set `flink.output-connector=iceberg`. | Required         | ondemand-index.properties              |
| sources                 | Comma separated list | Sources to re-enrich.  Every source must have a mapping in the index mapping file.  Duplicate values are ignored.                                                                                     | Required         | squid,dns                              |
| start.timestamp         | Timestamp            | Start of the time range.  Events with `ts` greater than or equal to this value are read.  Format `yyyy-MM-dd HH:mm:ss.SSS`.                                                                         | Required         | 2026-09-01 17:15:00.035                |
| end.timestamp           | Timestamp            | End of the time range.  Events with `ts` less than or equal to this value are read.  Must be later than `start.timestamp`.  Format `yyyy-MM-dd HH:mm:ss.SSS`.                                        | Required         | 2026-09-02 18:30:00.000                |
| destination.hive.dbname | Database name        | Iceberg database to write the enriched events to.  Must be different from the source database (`hive.dbname` in the index config) so the source index tables are not overwritten.                     | Required         | cyber_ondemand                         |
| report.errors.path      | File path            | Path to write rows that could not be converted back to a `Message`.  Written as one JSON record per line.  When running in Yarn, use an HDFS path the job can write to.                            | Required         | hdfs:/user/cybersec/ondemand/errors    |
| flink.job.name          | String               | Name of the job as it appears in the Flink dashboard.                                                                                                                                                | Default = `OnDemandTriage Job - default` | my_pipeline.ondemand                   |
| parallelism             | Integer              | Number of parallel tasks to run.                                                                                                                                                                     | Default = 2      | 4                                      |
| checkpoint.interval.ms  | Long                 | Milliseconds between Flink state checkpoints.  See [common checkpoint settings](../flink-common/README.md#checkpointing).                                                                            | Default = 60000  | 300000                                 |

### Referenced Index Configuration

The file named by `index.config` is the same file the indexing job uses.  The on-demand job requires
the following settings from it:

| Property Name          | Description                                                                                                            | Required/Default       | Example               |
|------------------------|------------------------------------------------------------------------------------------------------------------------|------------------------|-----------------------|
| flink.output-connector | Output connector.  Must be `iceberg`.                                                                                  | Required               | iceberg               |
| hive.dbname            | Source Iceberg database holding the indexed events.                                                                    | Default = cyber        | cyber                 |
| flink.mapping-file     | Path to the [index mapping file](../flink-indexing/flink-indexing-hive/README.md#mapping-file).                         | Required               | index-mapping.json    |
| flink.tables-init-file | Path to the [tables init file](../flink-indexing/flink-indexing-hive/README.md#tables-init-file) that defines the tables. | Required               | index-tables.json     |
| hive.confdir           | Directory holding the Hive client configuration (`hive-site.xml`, ...).                                                | Default = /etc/hive/conf | /etc/hive/conf      |

The tables init file must define the `unenriched_table_name` for each table that holds a source being
re-enriched, because the job reads the events back from those unenriched tables.

### Referenced Triage Configuration

The file named by `triage.config` is the same file the triaging job uses.  It supplies the enrichment
and scoring settings (geolocation, ASN, cidr, local lookups, HBase, rest, ThreatQ, Stellar, scoring
rules, schema registry, Kafka, ...).  See the [triaging job
README](../flink-enrichment/flink-enrichment-combined/README.md) for the full list of properties.

## Output Tables

The job writes the enriched events to the primary index tables in `destination.hive.dbname`.  The
schema is created from the same tables init and mapping files used by the indexing job, so the
destination tables match the primary index tables.  The unenriched tables are not written by this
job.

## Example

### On-Demand Properties File

```
# On-demand run settings
sources=squid
start.timestamp=2026-09-01 17:15:00.035
end.timestamp=2026-09-02 18:30:00.000
destination.hive.dbname=cyber_ondemand
report.errors.path=hdfs:/user/cybersec/ondemand/errors

# Referenced configurations
triage.config=ondemand-triage.properties
index.config=ondemand-index.properties

# Job settings
flink.job.name=BRANCH.PIPELINE.ondemand
parallelism=2
checkpoint.interval.ms=60000
```

### Referenced Index Properties File (ondemand-index.properties)

```
flink.output-connector=iceberg
hive.dbname=cyber
flink.mapping-file=index-mapping.json
flink.tables-init-file=index-tables.json
```

### Running the job

```
flink run --jobmanager yarn-cluster -yjm 2048 -ytm 2048 --detached --yarnname "On-Demand Triage" flink-ondemand-pipeline-2.4.0.jar ondemand.properties
```

If Hive, HBase or other client configuration is not in the default location, ship it with `-yt`:

```
flink run -yt hive-site.xml -yt hbase-site.xml -yt core-site.xml --jobmanager yarn-cluster -yjm 2048 -ytm 2048 --detached --yarnname "On-Demand Triage" flink-ondemand-pipeline-2.4.0.jar ondemand.properties
```

## Validation

The job validates its configuration on startup and fails to start if a check fails:

* The `flink.output-connector` in the index config is `iceberg`.  Other connectors are rejected.
* `start.timestamp` and `end.timestamp` match the format `yyyy-MM-dd HH:mm:ss.SSS` and the start time
  is earlier than the end time.
* Every source in `sources` has a mapping in the index mapping file.
* `destination.hive.dbname` is different from the source database (`hive.dbname`).
