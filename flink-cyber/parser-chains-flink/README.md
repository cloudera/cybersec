# Chaining Parser

Parses the raw text of an event, extracts fields and constructs a cyber Message.

## Chain Configuration

The parser chain defines how to convert a textual message to the fields of a structured cyber Message.

For example, the chain below converts a netflow message in json format, renames the @timestamp json element to timestamp, and converts the string timestamp to epoch millis.

Every message must produce a field called "timestamp" set to a long value of epoch milliseconds.  The timestamp field populates the ts field of cyber Message.
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

## Topic Map Configuration

The parser reads from one or more Kafka topics specified by the topic.pattern property.

The topic map determines which chain to use after reading a message from a topic.   The source field specifies the source of the resulting parsed cyber Message.

The example topic config below uses the netflow_type1 chain for all messages with topics starting with netflow_type1 and
uses the netflow_type2 chain for all messages with topics starting with netflow_type2.  Both parser chains result in a message with source type netflow.

```json
{ 
    "netflow_type1_.*" : { 
        "chainKey": "netflow_type1", 
        "source" : "netflow"
     }, 
     "netflow_type2_.*" : {
        "chainKey": "netflow_type2", 
         "source" : "netflow"
     }
}
```

## Error Handling
When a parser fails, the parser publishes a message to the parser.error topic with a single field containing the original text and a data quality error indicating the problem. 

In production, monitor the parser.error topic.  Determine the cause of the failures and correct the chain configuration.   Then replay the failed messages.

## Properties file
The job properties file would be of the form: 

```
kafka.bootstrap.servers=kafka.bootstrap.servers=<bootstrap>
kafka.client.id=parser-chain
kafka.group.id=parswr-chain
kafka.auto.offset.reset=latest

# chain and topic map configurations - see above
chain.file=/path/to/chain_config.json
chain.topic.map=/path/to/topic_config.json

# publish parsed messages to topic.output
topic.output=enrichment.input
# messages that fail to parse are published to topic.error
topic.error=parser.error
# read text messages from topics matching pattern below
topic.pattern=netflow.*


# Write the original string content to HDFS parquet files at the given location
original.enabled=true
original.basepath=/data/original/

# Signing key - used to sign the original content
key.private.file=private_key.der

schema.registry.url=https://schemareghost:7790/api/v1
schema.registry.client.ssl.trustStorePath=truststore.jks
schema.registry.client.ssl.trustStorePassword=truststorepassword
```