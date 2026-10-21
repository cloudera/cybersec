# On Demand Enrichment

This job re-enriches a bounded set of log events that have already been indexed, and writes the result
back through the Hive TableApi indexer. It is useful when the enrichment rules, reference data or scoring
rules changed and the historical events need to be brought up to date.

The pipeline runs in four steps:

1. read the log events between a start and an end timestamp that have one or more of the requested source
   values;
2. map every table row back to a `Message`;
3. run the shared `flink-enrichment-combined` enrichment and scoring chain;
4. hand the enriched `ScoredMessage` stream to the `flink-indexing-hive` TableApi indexer, which writes it
   to the output table.

## How to run

```
flink run -c com.cloudera.cyber.enrichment.ondemand.OnDemandEnrichmentJobKafka \
  flink-enrichment-ondemand-2.4.0.jar ondemand-enrichment.properties
```

Because the input is a bounded Hive query but the enrichment lookups and scoring rules are read from
unbounded Kafka topics, the job does not finish on its own. Stop it with `flink stop <jobId>` (or a
savepoint) once the log events have been processed.

## Configuration

All of the [combined enrichment](../../flink-enrichment-combined/README.md) settings still apply, because
this job reuses that pipeline. The enrichment command, ThreatQ and scoring rule topics must therefore be
configured.

| Parameter                | Default | Explanation                                                                                  |
|--------------------------|---------|----------------------------------------------------------------------------------------------|
| logevents.table          |         | Log events table to read, for example `cyber.events`.                                        |
| logevents.ts-column      | `ts`    | Timestamp column used for the range filter.                                                  |
| logevents.source-column  | `source`| Column used for the source filter.                                                           |
| logevents.start-ts       |         | Inclusive start of the range, as epoch milliseconds.                                         |
| logevents.end-ts         |         | Inclusive end of the range, as epoch milliseconds.                                           |
| logevents.sources        |         | Comma separated list of source values to re-enrich.                                          |
| hive.confdir             | `/etc/hive/conf` | Path to the hive-site.xml and core-site.xml config files.                            |
| hive.dbname              | `cyber` | Hive database that holds the log events and output tables.                                   |
| flink.writer             |         | Set to `TableAPI` to use the TableApi indexer.                                               |
| flink.output-connector   | `hive`  | TableApi output connector used to write the enriched messages.                               |
| flink.tables-init-file   |         | Output tables init file, see the [Hive indexer](../../flink-indexing/flink-indexing-hive/README.md). |
| flink.mapping-file       |         | Output mapping file, see the [Hive indexer](../../flink-indexing/flink-indexing-hive/README.md).     |

## Row to Message mapping

The read query selects `*` from the log events table. Columns are matched by name, so the order does not
matter:

* `id`, `ts`, `source` and `message` become the corresponding `Message` fields. `ts` accepts either a Hive
  timestamp or epoch milliseconds.
* the `fields` map column is copied into the message extensions;
* every other column, except the `originalsource_*` columns and the `dt` / `hr` partition columns, is added
  to the message extensions as a string;
* the original source coordinates are rebuilt from `originalsource_topic`, `originalsource_partition`,
  `originalsource_offset` and `originalsource_signature` when those columns are present. Otherwise a
  placeholder original source derived from `source` is used, because `Message.originalSource` is required.
