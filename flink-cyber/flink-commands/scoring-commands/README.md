# Scoring Rule Commands
## Update/Insert Scoring Rule
### Create Scoring javascript rule definition file threatq_rule.json:
```json
{
  "name": "ThreatQ Intel Match",
  "id" : "47a7759f-7b5a-456a-b064-a605a4633080",
  "order" : "1",
  "tsStart" : "2020-01-01T00:00:00.00Z",
  "tsEnd" : "2025-01-01T00:00:00.00Z",
  "type" : "JS",
  "ruleScript" : "if (message.containsKey(\"ip_src_addr.threatq.type\")) {return {score: 90.0, reason: 'ThreatQ intel match'};} else { return {score: 0.0, reason: 'no match'}};",
  "enabled" : true
}
```
### Create properties file
The properties file for the triaging or profiler jobs can be used as is.  The following properties are required:

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| schema.registry.url | url | Schema registry rest endpoint url | required | http://myregistryhost:7788/api/v1 |
| kafka.bootstrap.servers | comma separated list | Kafka bootstrap server names and ports. | required | brokerhost1:9092,brokerhost2:9092 |
| kafka.*setting name* | Kafka setting | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html).| set as required by security and performance | |
|query.input.topic| string | Produces a scoring rule to this topic in avro format with com.cloudera.cyber.scoring.ScoringRuleCommand schema.   | required | my-pipeline.scoring.input |
|query.output.topic|string | Consumes from this topic a response in avro format with com.cloudera.cyber.scoring.ScoringRuleCommandResult schema. | required | my-pipeline.scoring.output|
|rule.command.type|enum[UPSERT, ENABLE, DISABLE, DELETE, GET, LIST] | The type of command to send | UPSERT | |

### Run scoring rule upsert command
```shell script
usr/lib/jvm/java/bin/java  -Dlog4j.configuration=log4j.properties -Dlog4j.configurationFile=log4j.properties -cp scoring-commands-${CYBER_VERSION}.jar:slf4j-api-1.7.15.jar:log4j-1.2-api-2.17.2.jar:log4j-api-2.17.2.jar:log4j-core-2.17.2.jar:log4j-slf4j-impl-2.17.2.jar:/opt/cloudera/parcels/FLINK/lib/flink/lib/flink-dist_2.12-${FLINK_VERSION}.jar  com.cloudera.cyber.scoring.UpsertScoringRule ./triage.properties ./threatq_rule.json
```
