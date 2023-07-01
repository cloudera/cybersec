# Profile

The Profile stage of ingestion aggregates triaged messages over time and outputs the profile measurements to a kafka topic and optionally writes the messages to Apache Phoenix tables.
## Profile Configuration

The profile configuration includes one or more profile group.  Each profile group produces one message per period per key.  The message contains an extension for each measurement as well as any applicable stats for the measurement.  The profile group fields are as follows:
 
|Profile Group Field |Description |
|---------------------|----------------------------------------------------|
| profileGroupName | The name of the profile group.  Must be unique amongst all profile groups.|
| sources |A list of message source names to include in the profile.  If sources equals ANY, include all messages in the profile.|
|keyFieldNames | One or more extension names in the profile key. |
|periodDuration | Produce one profile message per key for this time period.|
|periodDurationUnit | The units for the periodDuration.  Valid values are MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS |
|statsSlide | When one of the measurements has calculateStats true, specify the time overlap between statistics time windows.  A new stats measurement is produced for each statsSlide window and includes all the measurements in the past period duration. |
|statsSlideUnit | The units for the statsSlide.   Valid values are MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS. |
|measurements |One or more measurements to calculate every period time window. See table below.|


|Profile Measurement Field | Description |
|--------------------------|-------------|
| fieldName | The name of the field to measure.   Required for all aggregation methods except COUNT. |
|resultExtensionName |Produce the profile measurement to this result. |
|aggregationMethod |The aggregation to perform on each field.   Valid values are COUNT, SUM, MIN, MAX, COUNT_DISTINCT, FIRST_SEEN |
|calculateStats | If true, include the min, max, mean, and standard deviation for all values of this measurement for all keys in the period. |
|firstSeenExpirationDuration | If the key has not appeared in events after this duration, report it as first seen. |
|firstSeenExpirationDurationUnit |The units for the firstSeenExpirationDuration.   Valid values are MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS. |

### Sample Profile Configuration
The profile below calculates two profile groups.  The profile group called ip_outgoing creates a message every 60 minutes a profile group called ip_outgoing.  All events with source netflow or squid in the period and non-empty values for ip_src_addr will be applied to the profile.  

Every 60 minutes, profile creates one ip_output message for each key ip_src_addr.

Every 60 minutes, profile also creates one domain_activity message for each domain.  The first seen profile tracks the first and last time a domain name appeared in a squid log message.

```json
[
  {
    "profileGroupName": "ip_outgoing",
    "sources": [
      "netflow",
      "squid"
    ],
    "keyFieldNames": [
      "ip_src_addr"
    ],
    "periodDuration": 60,
    "periodDurationUnit": "MINUTES",
    "statsSlide": 15,
    "statsSlideUnit": "MINUTES",
    "measurements": [
      {
        "fieldName": "dst_bytes",
        "resultExtensionName": "total_bytes_out",
        "aggregationMethod": "SUM",
        "calculateStats": true
      },
      {
        "fieldName": "ip_dst_addr",
        "resultExtensionName": "distinct_dest",
        "aggregationMethod": "COUNT_DISTINCT"      },
      {
        "fieldName": "total_transfer_bytes",
        "resultExtensionName": "transfer_size",
        "aggregationMethod": "SUM"      },
      {
        "fieldName": "domain",
        "resultExtensionName": "distict_domains",
        "aggregationMethod": "COUNT_DISTINCT"      }
    ]
  },
  {
    "profileGroupName": "domain_activity",
    "sources": [
      "squid"
    ],
    "keyFieldNames": [
      "domain"
    ],
    "periodDuration": 60,
    "periodDurationUnit": "MINUTES",
    "measurements": [
      {
        "resultExtensionName": "domain_first_seen",
        "aggregationMethod": "FIRST_SEEN",
        "firstSeenExpirationDuration": 365,
        "firstSeenExpirationDurationUnit": "DAYS"
      }    ]
  }
]
```

## Profile Validation
Profile validates the profile config file on startup. If the profile fails validation the profile job will not start.
### Profile json Syntax
Profile checks the syntax of the profile json to verify that it is correct.
### Profile Semantics
####Profile Group
* name specified and contains at least one character
at least one source
* name is unique among all profile groups
* period duration and period duration units specified
* period duration> 0
* period duration unit is a legal TimeUnit value
* has at least one measurement
* if at least one measurement calculates stats, statsSlide and statsSlideUnit defined
statsSlide > 0
* statsSlide unit is a legal TimeUnit value
* if no measurements calculate stats, statsSlide and statsSlideUnit should not be defined
#### Profile Measurement
* fieldName contains at least one character if aggregationMethod is not count or first seen
* resultExtensionName contains at least one character 
* resultExtensionName is unique among all resultExtensionNames in the profile group
* aggregationMethod specified
* if aggregationMethod is first seen
* verify firstSeenExpirationDuration is > 0 and firstSeenExpirationUnit is legal time unit
* if other aggregationMethod 
* verify neither firstSeenExpirationDuration nor firstSeenExpirationUnit is specified
### Profile Backward Compatibility
Profile assesses the backward compatibility of the profile groups defined in profile.json to the metadata in Phoenix.  Profile will not start if the profile.json is not backward compatible with the profile defined in the metadata.  If the validation check fails, profile reports an error and will not start.

The backward compatibility check assesses the following areas:
* profile group has equivalent period duration
* profile group has equivalent stats slide duration
* profile measurement result extension has same aggregation method and same field name

Equivalent time periods are two different ways of expressing the same duration using different units.  For example, 1 hour is equivalent to 60 minutes.

If profile rejects a modified profile group, rename the profile group or add a new profile group with a different name.  If the measurement result extensions
 are not compatible, rename the result extension or add a new result extension.  
 
Profile will only collect the profiles defined in profile.json.  If a profile group or result extension is not in the profile.json, profile will no longer collect measurements.  The existing measurements and metadata remain in Phoenix but profile no longer calculates or writes the measurement on new data.
 
## Properties Configuration

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| profile.config.file    | File path  | Json defining the profile groups and measurements.  See [profile configuration](#profile-configuration) above | Required | profile.json |
|profile.first.seen.table| legal HBase table name | Name of Hbase table to write first seen profiles values to | required | enrichments |
|profile.first.seen.column.family| legal HBase column family name | Name of Hbase column familty to write first seen profiles values to | required for metron format | first_seen |
|profile.first.seen.format | enum HBASE_METRON or HBASE_SIMPLE | Format used to store first and last seen time for first seen profiles | HBASE_METRON | HBASE_SIMPLE |
| phoenix.db.init | boolean | Enables or disables writing profile measurements and profile metadata to Phoenix. | required | true |
| phoenix.db.query.param.measurement_data_table_name | legal phoenix table name | Write measurement data to this phoenix table | required | prf_measure|
| phoenix.db.query.param.measurement_metadata_table_name | legal phoenix table name | Write profile measurement configuration metadata to this phoenix table | required | prf_measure_meta|
| phoenix.db.query.param.measurement_sequence_name | legal phoenix table name | Specifies table for tracking numeric identifiers for profiles | required |prf_seq|
|phoenix.db.query.param.profile_metadata_table_name|legal phoenix table name| Specified table for tracking profile metadata.|required|prf_meta|
 |phoenix.db.query.param.profile_sequence_name|legal phoenix table name|Specifies table for tracking automatically generated numeric identifiers for profiles.|required|prf_seq|
 |phoenix.db.query.param.measurement_sequence_start_with|positive integer|The starting value of the measurement automatically generated ids.|required|0|
 |phoenix.db.query.param.measurement_sequence_cache|positive integer|The number of sequence values to cache when creating profiles.  |required|20|
 |phoenix.db.query.param.profile_sequence_start_with|positive integer|The starting value of the profile automatically generated ids.|required|0|
 |phoenix.db.query.param.profile_sequence_cache|positive integer|The number of sequence values to cache when creating profiles.  |required|20|
 |phoenix.db.query.param.field_key_count|positive integer| Maximum number of key fields in all profile groups |required|10|
|phoenix.db.batchSize|Positive integer >=1|Number of measurements in a phoenix batch.|required|40|
|phoenix.db.interval_millis|Number of milliseconds before writing a batch.| Positive integeger >= 0|required|60000|
|phoenix.db.max_retries_times|Positive integer >= 1|Maximum number of times to try writing to Phoenix before failing. |required|3|
| topic.output | topic name | Outgoing triaged profile messages.  Stored in AVRO message format managed by schema registry. | required | profile.output |
| topic.input | topic name | Incoming triaged messages to be profiled.  Stored in AVRO message format managed by schema registry. | required | triage.output |
| query.input.topic | topic name | Modify the scoring rules applied to each profile message.  Stored in AVRO scoring rule command format managed by schema registry. | required | profile.scoring.input |
| query.output.topic | topic name | Scoring rule result messages indicating that profile received the scoring rule.  Stored in AVRO scoring command result format managed by schema registry. | required | profile.scoring.output |
| parallelism | integer | Number of parallel tasks to run.  | default=2 | 2 |
| checkpoint.interval.ms | integer | Milliseconds between Flink state checkpoints | default=60000 | 10000|
| schema.registry.url | url | Schema registry rest endpoint url | required | http://myregistryhost:7788/api/v1 |
| kafka.bootstrap.servers | comma separated list | Kafka bootstrap server names and ports. | required | brokerhost1:9092,brokerhost2:9092 |
| kafka.*setting name* | Kafka setting | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html).| set as required by security and performance | |
| flink.job.name | String | Set the Flink job name | defaults to Flink Parser | proxy parser |

### Example Properties file

```
# Kafka 
kafka.bootstrap.servers=kafka.bootstrap.servers=<bootstrap>
kafka.client.id=profile
kafka.group.id=profile
kafka.auto.offset.reset=latest

# Schema Registry
schema.registry.url=https://schemareghost:7790/api/v1
schema.registry.client.ssl.trustStorePath=truststore.jks
schema.registry.client.ssl.trustStorePassword=truststorepassword

# Profile configuration
profile.config.file=PIPELINE/profile/PROFILE_NAME/profile.json

# first seen Hbase storage 
profile.first.seen.table=enrichments
profile.first.seen.column.family=first_seen

# enable/disable writing measurements to phoenix
phoenix.db.init=true

# phoenix measurement data and metadata table names
phoenix.db.query.param.measurement_data_table_name=prf_measure
phoenix.db.query.param.measurement_metadata_table_name=prf_measure_meta
phoenix.db.query.param.measurement_sequence_name=prf_measure_seq
phoenix.db.query.param.profile_metadata_table_name=prf_meta
phoenix.db.query.param.profile_sequence_name=prf_seq
phoenix.db.query.param.measurement_sequence_start_with=0
phoenix.db.query.param.measurement_sequence_cache=20
phoenix.db.query.param.profile_sequence_start_with=0
phoenix.db.query.param.profile_sequence_cache=20
phoenix.db.query.param.field_key_count=10

# phoenix batching and retry
phoenix.db.batchSize=40
phoenix.db.interval_millis=60000
phoenix.db.max_retries_times=3

# topics
topic.output=BRANCH.PIPELINE.profile.PROFILE_NAME.output
topic.input=BRANCH.PIPELINE.triage.output
query.input.topic=BRANCH.PIPELINE.profile.PROFILE_NAME.scoring.input
query.output.topic=BRANCH.PIPELINE.profile.PROFILE_NAME.scoring.output

# job parameters
flink.job.name=BRANCH.PIPELINE.profile.PROFILE_NAME
parallelism=1
checkpoint.interval.ms=60000
```

## Running the job

```
flink run --jobmanager yarn-cluster -yjm 2048 -ytm 2048 --detached --yarnname "Profile" flink-profiler-java-0.0.1-SNAPSHOT.jar profile.properties
```