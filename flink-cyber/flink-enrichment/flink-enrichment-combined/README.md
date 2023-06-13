# Triaging Job 

## Enrichment and Scoring Pipeline
Applies selected enrichments to a message in the following order:
1. Maxmind IP Geolocation.
2. Maxmind IP ASN.
3. IP Cidr
4. Local Flink state lookup.
5. HBase key lookup.
6. Rest service results.
7. Experimental Feature: [Stix 1.x](https://oasis-open.github.io/cti-documentation/stix/compare) threat intelligence indicators.
8. Threatq threat intelligence indicators.
9. Stellar.

After applying all enrichments, the triaging job runs all scoring rules and attaches the scores to the event.
The triaging job publishes the scored event to the output topic.

## Enrichment Loading
The triaging job consumes EnrichmentCommand avro messages from the enrichment input topic, stores the enrichment in either Flink 
state or in HBase depending on the enrichment type and publishes an EnrichmentCommandResponse message to the enrichment output topic.
When loading enrichments, the job uses the [enrichment configuration](../../flink-cyber-api/enrichment_config.md) and [enrichment storage](../flink-enrichment-lookup-hbase/enrichment_json.md) files to determine how to store the enrichment.
* If the enrichment_configuration file specifies that the enrichment is LOCAL, the enrichment is ingested into flink state.
* If the enrichment_configuration file specified that the enrichment is HBASE, the enrichment is ingested into HBase and the enrichment_storage file determines the table, column family and format.
* If the enrichment is not specified in either file, the enrichment is ingested into HBase.  The enrichment_storage file determines the table, column family and format.

Use the [flink-enrichment-loading job](../flink-enrichment-load/README.md) with the enrichment.topic.input property set to the enrichment input topic for the triaging job.  The flink-enrichment-loading job will create the avro records and publish them to Kafka in the correct format. 

## Scoring
Use the [scoring-command](../../flink-commands/scoring-commands/README.md) java program to send a scoring rule.  After upserting the scoring rule, the 
triaging job will run the scoring rule for each event and attach the scores to the events.

## Error Handling
If a recoverable error occurs during one of the enrichment steps, a data quality message will be added to the message incidating the step that failed and the reason for failure. 
The triaging output topic should be monitored for data quality errors.

# Configuration Properties

## General Properties Configuration

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| parallelism | integer | Number of parallel tasks to run.  | default=2 | 2 |
| checkpoint.interval.ms | integer | Milliseconds between Flink state checkpoints | default=60000 | 10000|
| schema.registry.url | url | Schema registry rest endpoint url | required | http://myregistryhost:7788/api/v1 |
| kafka.bootstrap.servers | comma separated list | Kafka bootstrap server names and ports. | required | brokerhost1:9092,brokerhost2:9092 |
| kafka.*setting name* | Kafka setting | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html).| set as required by security and performance | |
| flink.job.name | string | Set the Flink job name as it will appear in the Flink dashboard. | Triaging Job - default | my_pipeline.triage |
| topic.input | string | Consumes input messages from this topic.   Topic should contain com.cloudera.cyber.Messages in avro format| required | my_pipeline.triaging.input |
| topic.output | string | Publishes output messages to this topic.   The triaging job produces messages in com.cloudera.cyber.scoring.ScoredMessage avro format | required | my_pipeline.triaging.output |
|query.input.topic| string | Consumes new or modified scoring rules from this topic in avro format using the com.cloudera.cyber.scoring.ScoringRuleCommand schema.   | required | my-pipeline.scoring.input |
|query.output.topic|string | Produces responses to scoring rules in avro format using the com.cloudera.cyber.scoring.ScoringRuleCommandResult schema. | required | my-pipeline.scoring.output|

## HBase configurations
If Flink, Yarn and HBase are running on the same cluster, the enrichment job will read the hbase-site.xml, core-site.xml, and hdfs-site.xml from the default Cloudera Manager location in /etc/hbase/conf.
If Flink is accessing HBase on a different cluster, download the HBase client configuration files from the HBase cluster Cloudera Manager 
(Cluster > Actions menu > View Client Configuration URLs and click on the HBASE url to download the configuration files).  Ship the config files by adding the -yt to the flink run command:

flink run -yt hbase-site.xml -yt hdfs-site.xml -yt core-site.xml flink-enrichment-combined.jar triaging.properties

## Maxmind Geocoding properties

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|geo.enabled    | boolean | If true, look up the geolocation for the specified geo.ip_fields and add to the location to the message.  Otherwise, skip geocoding. | true | false |
|geo.ip_fields | string | Comma separated list of field names to perform geocoding.  If the field is set to an IP address, look up the ip in the maxmind database.  Add the geolocation information to the event.   | required | ip_src_addr,ip_dst_addr|
|geo.database.path | string | Path to the Maxmind geolocation .mmdb file.  If running in yarn, use an HDFS location so the flink job can access the file.  The flink job user must have read access to the file. | required | hdfs://cyber/geo/GeoLite2-City.mmdb |

## Maxmind ASN properties               

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|asn.enabled    | boolean | If true, look up the ASN for the specified asn.ip_fields and add to the ASN to the message.  Otherwise, skip ASN enrichment. | true | false |
|asn.ip_fields | string | Comma separated list of field names to perform ASN lookup.  If the field is set to an IP address, look up the ip in the Maxmind ASN database.  Add the ASN information to the event.   | required | ip_src_addr,ip_dst_addr|
|asn.database.path | string | Path to the Maxmind ASN .mmdb file.  If running in yarn, use an HDFS location so the flink job can access the file.  The flink job user must have read access to the file. | required | hdfs://cyber/geo/GeoLite2-ASN.mmdb |

## IP Cidr 

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|cidr.enabled    | boolean | If true, enrich the specified message fields with matching Cidrs defined in the cidr.config_file_path.   Otherwise, skip cidr enrichment. | true | false |
|cidr.ip_fields | string | Comma separated list of field names to perform Cidr lookup.  If the field is set to an IP address, add the names of any matching Cidr ranges. | required | ip_src_addr, ip_dst_addr |
|cidr.config_file_path| string | Path to the [configuration file](../flink-enrichment-cidr/README.md) defining the Cidr ranges.  | required |hdfs:/user/flink/data/enrichments-cidr.json |

## Local Flink State Lookup

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|lookups.config.file | string | Path to the [json configuration file](../../flink-cyber-api/enrichment_config.md) specifying the enrichments to apply to each source. | required | enrichments.json |
|enrichment.topic.input | string | Name of topic to consume local and hbase enrichment commands that create or update enrichment key value mappings.  Topic must contain messages of type com.cloudera.cyber.commands.EnrichmentCommand | required | my_pipeline.enrichments.input|
|enrichment.topic.query.output | string | Publish enrichment command results to this topic.  | required | my_pipeline.enrichments.output |

## HBase Key Lookup

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|hbase.enabled | boolean | If true, enrich messages with key-value mappings stored in HBase | true | false |
|lookups.config.file | string | Path to the [json configuration file](../../flink-cyber-api/enrichment_config.md) specifying the enrichments to apply to each source. | required | enrichments.json |
|enrichment.topic.input | string | Name of topic to consume local and hbase enrichment commands that create or update enrichment key value mappings.  Topic must contain messages of type com.cloudera.cyber.commands.EnrichmentCommand | required | my_pipeline.enrichments.input|
|enrichment.topic.query.output | string | Publish enrichment command results to this topic.  | required | my_pipeline.enrichments.output |

## Rest Service Results
| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|rest.enabled | boolean | If true, enrich messages with key-value mappings stored in HBase | true | false |
|rest.config.file | string |Path to the [rest configuration file](../flink-enrichment-lookup-rest/README.md) specifying the res enrichments to apply to each source.  | required | rest-enrichments.json|

## Stix 1.x Threat Intelligence Indicators

NOTE: This feature is experimental and for development environments only.  Do not use in production.

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|stix.enabled| boolean | If true, enrich message with Stix threat intelligence | true | false|
|stix.input.topic| string | Topic to consume new Stix 1.x threat intelligence indicators. | stix|my_pipeline.stix.input|
|stix.output.topic|string | Topic to publish results of Stix threat indicators | stix.output | my_pipeline.stix.output|
|stix.hbase.table| string | Store Stix threat intelligence in this hbase table | threatIntelligence | stix_ti |


## Threatq Threat Intelligence Indicators
    
| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|threatq.enabled| boolean | If true, enrich message with Threatq threat intelligence | true | false|
|threatq.config.file| string | Path to file defining which Threatq indicators to apply to message fields. | required | threatq.json|
|threatq.topic.input| string | Publish new threatq indicators to this topic.  The job ingests the indicators, stores them in hbase, and then applies them to new messages. | required |

## Stellar 

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
|stellar.enabled| boolean | If true, enrich message with Metron Stellar enrichments | true | false|
|stellar.config.dir | string | Directory with Metron enrichment configuration json files.   When running in yarn, directory must be shipped using the -yt parameter to the flink run command or stored in HDFS.  | required | enrichments|
|geo.database.path | string | Path to the Maxmind geolocation .mmdb file for Metron geocode enrichments.  If running in yarn, use an HDFS location so the flink job can access the file.  The flink job user must have read access to the file. | required | hdfs://cyber/geo/GeoLite2-City.mmdb |
|asn.database.path | string | Path to the Maxmind ASN .mmdb file for Metron ASN enrichments.  If running in yarn, use an HDFS location so the flink job can access the file.  The flink job user must have read access to the file. | required | hdfs://cyber/geo/GeoLite2-ASN.mmdb |

#Running the Triaging Job
* Construct a 'triage.properties' file using the configuration options above.

```
# general job
flink.job.name=my-pipeline.triage
parallelism=1
checkpoint.interval.ms=60000

# kafka and schema registry 
kafka.bootstrap.servers=cybersec-1.vpc.cloudera.com:9092,cybersec-1.vpc.cloudera.com:9092
kafka.acks=all
kafka.client.id=my-pipeline-triage
kafka.group.id=my-pipeline-triage
schema.registry.url=http://cybersec-1.vpc.cloudera.com:7788/api/v1

# topics
topic.output=my-pipeline.triage.output
topic.input=my-pipeline.triage.input
enrichment.topic.input=my-pipeline.enrichments.input
enrichment.topic.query.output=my-pipeline.enrichment.output
query.input.topic=my-pipeline.scoring.rules.input
query.output.topic=my-pipeline.scoring.rules.output

# geo
geo.enabled=true
geo.ip_fields=ip_src_addr,ip_dst_addr,ip_dst,ip_src,not_defined
geo.database_path=hdfs:/user/flink/data/GeoLite2-City.mmdb

# asn
asn.enabled=true
asn.ip_fields=ip_src_addr,ip_dst_addr,not_defined,ip_dst,ip_src
asn.database_path=hdfs:/user/flink/data/GeoLite2-ASN.mmdb

# cidr
cidr.enabled=false
cidr.ip_fields=ip_src_addr,ip_dst_addr
cidr.config_file_path=hdfs:/user/flink/data/enrichments-cidr.json

# lookups
lookups.config.file=enrichments.json
hbase.enabled=true
enrichments.config=enrichments_storage.json

# rest
rest.enabled=true
rest.config.file=enrichment-rest.json

# stellar
stellar.enabled=true
stellar.config.dir=enrichments

# disabled enrichments
stix.enabled=false
rules.enabled=false
threatq.enabled=false
```
*  Run the triaging job using the flink run command.  If using stellar enrichments, use the -yt command to ship the stellar enrichment configuration files to yarn as below.

```
flink run \
-yt enrichments \
 --jobmanager yarn-cluster -yjm 3072 -ytm 3072  --detached --yarnname "my_pipeline.triage" flink-enrichment-combined-2.3.0.jar triage.properties
```