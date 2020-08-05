# Caracal Splitting Parser

This parser takes topics with raw JSON and applies two jsonpath expressions to it.

The `headerPath` results are combined with `splitPath` results to form a message per split on the output topic.

You can also specify a `timestampField` from the header (TODO - expose the split source version in the config) which determines the field to extract as the message `ts` (timestamp). You can also apply javascript transformation to the timestamp. The transformation function should return a long value of epoch milliseconds.

The topic field is the source json topic, and will be recorded as the `source` field of the outbound message.
### Example
```json
[
  {
    "topic": "dpi_http",
    "splitPath": "$.http-stream['http.request'][*]",
    "headerPath": "$.http-stream",
    "timestampField" : "start_ts",
    "timestampFunction": "Math.round(parseFloat(ts)*1000,0)"
  },
  {
    "topic": "dpi_dns",
    "splitPath": "$.dns-stream['dns.dns_query'][*]",
    "headerPath": "$.dns-stream",
    "timestampField" : "start_ts",
    "timestampFunction": "Math.round(parseFloat(ts)*1000,0)"
  },
  {
    "topic": "dpi_radius",
    "splitPath": "$.radius-stream['radius.request'][*]",
    "headerPath": "$.radius-stream",
    "timestampField" : "start_ts",
    "timestampFunction": "Math.round(parseFloat(ts)*1000,0)"
  },
  {
    "topic": "dpi_smtp",
    "splitPath": "$.smtp-stream['smtp.email'][*]",
    "headerPath": "$.smtp-stream",
    "timestampField" : "start_ts",
    "timestampFunction": "Math.round(parseFloat(ts)*1000,0)"
  }
] 
```

## Additional options

As with many jobs, you need to provide kafka connection and some job parameters.

This is provided as a properties file to the command line for launching the flink job.

### Example properties file
```json
kafka.bootstrap.servers=<bootstrap>
kafka.client.id=caracal-splits
kafka.group.id=caracal-splits
kafka.auto.offset.reset=earliest

config.file=<./path/to/json/above.json>
topic.output=caracal.split

registry.address=http://<registry-server>:7788/api/v1
```

Other options worth considering:
```checkpoint.interval.ms=10000``` checkpoint every 10 seconds
```parallelism=2``` default job parallelism

## Running the job

```
flink run -Dlog4j.configurationFile=log4j.properties --jobmanager yarn-cluster -yjm 1024 -ytm 1024 --detached --parallelism 2 --yarnname "Caracal Parser" caracal-parser-0.0.1-SNAPSHOT.jar caracal-parser.properties
```