# Chaining Parser

## Parsing raw text and converting to structured messages
The chaining parser reads the raw text of an event, extracts fields into a consistent schema and constructs a cyber Message.  The cyber Message is passed on to the enrichment and triaging phase of the pipeline.

## Extracting streaming enrichments from structured messages
If chain.enrichments.file is set, the parser converts the message fields to enrichment keys and values and writes the enrichments to HBase. See [EnrichmentsConfiguration](../flink-enrichment/flink-enrichment-lookup-hbase/enrichment_json.md). 

## Archiving original message content
If original.enabled is true, the parser aggregates the original message content along with the original topic, partition and offset in Parquet files. 
The parser writes the Parquet files to the path specified in original.basepath.  The basepath supports HDFS, Ozone or a cloud store.

## Signing original message content
If signature.enabled is true, the parser signs each original message content and includes the signature in the structured message.  
This feature is compute intensive and only recommended for low events per second deployments with anti-tamper requirements.

## Chain Configuration

The parser chain configuration defines how to convert a textual message to the fields of a structured cyber Message.

For example, the chain below reads a netflow message in json format, renames the @timestamp json element to timestamp, and converts the string timestamp to epoch millis.

Every message must produce a field called "timestamp" set to a long value of epoch milliseconds.  The timestamp field populates the ts field of cyber Message.
### Sample Chain Configuration
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

The topic map defines which topics the parser will consume and how the raw data will be converted to structured messages.

The topic map determines which chain to use after reading a message from a topic.   The source field specifies the source of the resulting parsed cyber Message.

The topic map can optionally define the broker hosting the topic.  If the broker is not specified, the topic is read from the default kafka broker.

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

If the parser is extracting streaming enrichments, the parser will send a message to the error topic if an enrichment key field is missing or no enrichment values can be extracted from the message. 

In production, monitor the parser.error topic.  Determine the cause of the failures and correct the chain configuration.   Then replay the failed messages.

## Properties Configuration

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| chain.file    | File path  | Json defining the parser chains and their tasks.  See [parser chain configuration](#parser-chain-configuration) for details | Required | parser-chain.json |
| chain         | String     | Alternate to chain.file for test cases.  Specify the [parser chain configuration](#parser-chain-configuration) inline rather than a separate file.   | Optional | parser chain json content |
| chain.topic.map.file | File path | Json defining the mapping between topics and the parser chains used to parser the raw messages. | Required | topic-map.json |
| chain.topic.map | String | Alternative to chain.topic.map.file.  | Optional | {"squid.*" : {"chainKey": "squid", "source" : "proxy"}} |
| chain.enrichments.file | File path | Defines [EnrichmentsConfigFile](../flink-enrichment/flink-enrichment-lookup-hbase/enrichment_json.md) mapping from messages to enrichment key and values.   Defines storage format for enrichment. | Optional - if not specified, no enrichments extracted from messages. | enrichment-storage.json |
| topic.output | topic name | Outgoing parsed and structured messages.  Stored in AVRO message format managed by schema registry. | required | triage.input |
| original.enabled | Boolean | If true, archive the original message text.  | true by default | false |
| original.basepath | File path | Archive the original messages partitions at this location in Parquet format. | required when original.enabled = true | hdfs:/path/to/original | 
| signature.enabled | Boolean | Determines if original message content will be signed.  If enabled, parser performs at much lower events per second. | defaults to true | false |
| key.private.file | File path | Path to private key .der file used to sign original messages. | required if signature.enabled=true | private-key.der |
| key.private.base64 | String | For testing.  Base64 encoding of private key.  | optional | |
| parallelism | integer | Number of parallel tasks to run.  | default=2 | 2 |
| checkpoint.interval.ms | integer | Milliseconds between Flink state checkpoints | default=60000 | 10000|
| schema.registry.url | url | Schema registry rest endpoint url | required | http://myregistryhost:7788/api/v1 |
| kafka.bootstrap.servers | comma separated list | Kafka bootstrap server names and ports. | required | brokerhost1:9092,brokerhost2:9092 |
| kafka.*setting name* | Kafka setting | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html).| set as required by security and performance | |
| \<broker\>.kafka.bootstrap.servers | comma separated list | [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html) for non-default broker referenced in [TopicMap](#topic-map-json-configuration). | required if topic map references non-default broker | network_broker|
| \<broker\>.kafka.*setting name* | Kafka setting | Settings for [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html) for non-default broker referenced in [TopicMap](#topic-map-json-configuration). | set as required by security and performance | |
| flink.job.name | String | Set the Flink job name | defaults to Flink Parser | proxy parser |

### Example Properties file

```
kafka.bootstrap.servers=kafka.bootstrap.servers=<bootstrap>
kafka.client.id=parser-chain
kafka.group.id=parser-chain
kafka.auto.offset.reset=latest

# chain and topic map configurations - see above
chain.file=/path/to/chain_config.json
chain.topic.map.file=/path/to/topic_config.json

# publish parsed messages to topic.output
topic.output=enrichment.input
# messages that fail to parse are published to topic.error
topic.error=parser.error

# Write the original string content to HDFS parquet files at the given location
original.enabled=true
original.basepath=/data/original/

# Disable signing for better performance
signature.enabled=false

schema.registry.url=https://schemareghost:7790/api/v1
schema.registry.client.ssl.trustStorePath=truststore.jks
schema.registry.client.ssl.trustStorePassword=truststorepassword
```

## Parser Chain Configuration
The parser chain configuration file is a map from chain name to chain definition.   Each chain definition specifies the ordered set of steps to convert raw log messages into structured messages. 

### Parser Chain Json Fields

| Json Field       | Type                              | Description                                                                                                           | Required/Default | Example                                |
| -----------------| --------                          | --------------------------                                                                                            | ---------------- | -------------------------------------- |
| chain name       | string                            | Name of the parser chain.  Must be unique among all chains defined in context.  Refer to the chain by this name in the topic map.  | required | netflow                         |
| id               | string                            | Identifier of the parser.  Can be UUID or unique number.                                                              | required | 1 or 220ee8c5-07d7-48d9-8df5-7d23376cb664 |
| name             | string                            | Human readable name of the parser.                                                                                    | required  | Netflow Parser |
| parsers          | array of [parser tasks](#parser-task-json-fields) | Ordered list of parser tasks to convert a raw log to a structured message.                           | required  | See [sample chain](#sample-chain-configuration)|

### Parser Task Json Fields

| Json Field       | Type                              | Description                                                                                                           | Required/Default | Example                                |
| -----------------| --------                          | --------------------------                                                                                            | ---------------- | -------------------------------------- |
| id               | string                            | Identifier of the parser.  Can be UUID or unique number.                                                              | required | 1 or 220ee8c5-07d7-48d9-8df5-7d23376cb664 |
| name             | string                            | Human readable name of the parser.                                                                                    | required  | Netflow Parser |
| type             | parser class name  | Parser task class name.                           | required  | com.cloudera.parserchains.parsers.JSONParser |
| config           | parser specific configuration | Configuration for parser.  Definitions vary based on parser type. | required | See [sample chain](#sample-chain-configuration) |

## Topic Map Json Configuration

The topic map defines which topics the parser consumes and how the messages in the topic are converted to structured messages.

| Json Map         | Type                              | Description  | Required/Default | Example
| -----------------| --------                         | --------------------------                                                                                            | ---------------- | -------------------------------------- |
| topic pattern key | [Java regex ](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) | Pattern specifying the topic or topics containing raw messages | Required | netflow.* |
| chainKey          | String                           | Name of the chain key used to convert the raw messages to structured messages.   | Required | squid |
| source            | String                           | The name of the source for messages produced by this mapping.  | Required | proxy |
| broker            | String                           | The name of the broker hosting the topics. Define the broker connection parameters using <broker>.kafka.<parameter name>| Default - use same broker as parser | externalBroker | 

## Creating the signing key
Below are example openssl commands to create the private key for signing the messages.  Consult the cryptographic requirements of your organization before creating these files.
```shell script
openssl genrsa -out private_key.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt
openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der
```

## Data Quality Messages
The parser reports the following messages. 
 
| Severity Level | Feature | Message | Meaning |
| ---------------| ------- | ------- | ------- |
| ERROR           | chain_parser     |  Message does not contain a timestamp field. | The parsed message does not contain a field named timestamp |
| ERROR           | chain_parser     | Timestamp is not in epoch milliseconds or seconds. | The message has a field named timestamp but it is either not numeric or is not in epoch milliseconds or seconds |
| ERROR           | stream_enrich    | Message does not define values for key field for enrichment type '\<enrichment_type\>'" | An enrichment could not be extracted from the parsed message because the message did not contain one of the required key fields |
| ERROR           | stream_enrich    | Message does not contain any values for enrichment type '\<enrichment_type\>' | There were no values to write for the enrichment.  Either the message did not contain extensions or the message did not specify any of the value fields. |

## Running the job

```
flink run --jobmanager yarn-cluster -yjm 1024 -ytm 1024 --detached --yarnname "Parser" parser-chains-flink-0.0.1-SNAPSHOT.jar parser.properties
```