# Scoring Rule Commands
## Update/Insert Scoring Rule
### Create Scoring rule definition file
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
kafka.{kafka properties}: All properties required to produce to the kafka brokers
query.input.topic: name of the topic accepting scoring rule commands
query.ouptut.topic: name of the topic where scoring rule command results are written by the scoring engine
### Run scoring rule upsert command
```shell script
java -cp scoring-commands-0.0.1-SNAPSHOT.jar:slf4j-api-1.7.15.jar  com.cloudera.cyber.scoring.UpsertScoringRule ./profiler.properties ./sample_rule.json
```
