# Enrichment

This is a raw flink implementation of local broadcast enrichment lookups.

Config for the enrichment process is as follows: 
```json
[
  {
    "source": "test",
    "kind": "LOCAL",
    "fields": [
      {
        "name": "ip_src_addr",
        "enrichmentType": "ip_whitelist"
      },
      {
        "name": "ip_dst_addr",
        "enrichmentType": "ip_whitelist"
      }
    ]
  }
]
```

This defines one enrichment type and two fields that would be enriched from messages with source value "test"

Enrichment data itself is fed onto another kafka stream. 

```
# Config file from above
config.file=config.json

kafka.bootstrap.servers=<bootstrap-server:9093>
kafka.client.id=enrichment-lookups-local
kafka.group.id=enrichment-lookups-local
kafka.acks=all

topic.input=<topic to source messages from>
topic.output=<output topic for enriched messages>

enrichment.topic.input=<topic with all the EnrichmentEntry messages on>

```

Note that this process is VERY stateful. You should usually restart it from a previous savepoint and DO NOT use any auto offset resets unless you really know what you are doing.
