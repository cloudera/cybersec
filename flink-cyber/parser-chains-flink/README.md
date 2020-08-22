# Chaining Parser

Config is a topic to parser config map using a full parser chain.

For example, to process data in a json form in a topic called `netflow` would run the chain here.
```json
{
    "netflow": {
        "id": "220ee8c5-07d7-48d9-8df5-7d23376cb664",
        "name": "Netflow Parser",
        "parsers": [
            {
                "id": "f812c6dc-40cc-4c77-abf8-e15fccdfea32",
                "name": "Netflow as JSON",
                "type": "com.cloudera.parserchains.parsers.JSONParser",
                "config": {
                    "input": {
                        "input": "original_string"
                    },
                    "norm": {
                        "norm": "UNFOLD_NESTED"
                    }
                }
            },
            {
                "id": "6b8797a2-95df-4021-83c2-60ac4c786e67",
                "name": "Field Renamer",
                "type": "com.cloudera.parserchains.parsers.RenameFieldParser",
                "config": {
                    "fieldToRename": [
                        {
                            "from": "@timestamp",
                            "to": "timestamp"
                        }
                    ]
                }
            },
            {
                "id": "9549004f-83e4-4d24-8baa-abdbdad06e61",
                "name": "Timestamp Parser",
                "type": "com.cloudera.parserchains.parsers.TimestampFormatParser",
                "config": {
                    "fields": [
                        {
                            "field": "timestamp",
                            "format": "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            "tz": "UTC"
                        }
                    ]
                }
            }
        ]
    }
}
```

The job properties file would be of the form: 

```
kafka.bootstrap.servers=kafka.bootstrap.servers=<bootstrap>
kafka.client.id=parser-chain
kafka.group.id=parswr-chain
kafka.auto.offset.reset=latest

config.file=/path/to/config.json
topic.output=enrichment

# Write the original string content to HDFS parquet files at the given location
original.enabled=true
original.basepath=/data/original/

# Signing key - used to sign the original content
key.private.file=private_key.der

registry.address=http://schema.registry.server:7788/api/v1
```