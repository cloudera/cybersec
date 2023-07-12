# Hive Indexer

This job takes Message objects from a Kafka Topic and translates them to fit into a Hive Table. There's two writer options that allow to change how Indexer behaves: TableApi and default.

To specify non-default the writer, you need to update index.properties file to have ```flink.writer``` property, and set it to one of the allowed values: (tableapi). If you'll leave the property empty, remove it from the config, or provide a value that's not present in the allowed values, the default writer will be selected.  

## Default writer

The process reads the structure of the hive table and extracts the fields required to populate it from the message. It also expects a minimum set of fields in the hive table, and a column which accepts a map to contain any non-extracted fields.

For example, the following hive table schema could be used as a destination:
```SQL
SET hive.txn.manager=org.apache.hadoop.hive.ql.lockmgr.DbTxnManager;

 use cyber;
 CREATE TABLE `events`(                       
    `id` string,                                     
    `ts` timestamp,                                  
    `message` string,                                
    `fields` map<string,string>,                     
    `ip_src_addr` string,                            
    `ip_dst_addr` string,                            
    `ip_src_port` string,                            
    `ip_dst_port` string,                            
    `source` string,                                 
    `ip_dst_addr_geo_latitude` double,               
    `ip_dst_addr_geo_longitude` double,              
    `originalsource_topic` string,                   
    `originalsource_partition` int,                  
    `originalsource_offset` bigint,                  
    `originalsource_signature` binary,               
    `ip_dst_addr_geo_country` string,                
    `ip_dst_addr_geo_city` string,                   
    `ip_dst_addr_geo_state` string,                  
    `dns_query` string,                              
    `cyberscore` float,                              
    `cyberscore_details` array<struct<`ruleid`:string, `score`:float, `reason`:string>>) 
  PARTITIONED BY (                                   
    `dt` string,                                     
    `hr` string)                                     
  CLUSTERED BY (                                     
   source)                                          
  INTO 2 BUCKETS                                     
  stored as orc tblproperties("transactional"="true");

```
Table requirements:
1. The table must be transactional and meet all the requirements for [Hive Streaming V2](https://cwiki.apache.org/confluence/display/Hive/Streaming+Data+Ingest+V2). 
2. The hive-indexer Flink job must have select and insert permission on the table.
3. The hive-indexer flink job must have permission to write to the HDFS directory storing the hive managed table. For example hdfs://mycluster:8020/warehouse/tablespace/managed/hive/cyber.db/events
4. The hive-indexer supports the following types in columns mapping to extensions:

|hive column type|conversion from extension string|
| --- | --- |
|string| no conversion necessary |
|bigint| convert string to long|
|int| convert string to int|
|double | convert string to double |
|float | convert string to float |
|boolean | convert string to boolean|
|timestamp| if timestamp is a long convert epoch milliseconds to timestamp.  Otherwise parse timestamp string using patterns in specified by hive.timestamp.format property.  If none of these conversions are successful, the timestamp extension will be written to the fields map.|  

Transactions:
The streaming writer creates a transaction and keeps adding events to the transaction until the indexer reaches hive.transaction.messages and when a checkpoint is reached.

| parameter | default | explanation |
| --- | --- | --- |
|hive.confdir | /etc/hive/conf | Path to the hive-site.xml and core-site.xml config files. |
| hive.table | events | Table to publish messages to |
| hive.catalog | default | A catalog to store events in |
| hive.schema | cyber | The name of the database in Hive |
|hive.batch.size | 1 | [Hive Streaming transaction batch size](http://hive.apache.org/javadocs/r3.0.0/api/org/apache/hive/streaming/HiveStreamingConnection.Builder.html#withTransactionBatchSize-int-) | 
| hive.transaction.messages | 1000 | Number of messages to include in a transaction. |
| hive.timestamp.format | see defaults below table | Comma separated list of [SimpleDateFormats](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) for converting timestamp extension strings to timestamp hive columns |
|||
| topic.input || Source topic in Kafka |
| kafka.*|| Ability to override Kafka Consumer settings |
| schema.registry.url|http://<schema-registry-host>:7788/api/v1| location of schema registry|
| schema.registry.client.ssl|| whether to use ssl for SR |
| trustStorePath|| Location of truststore for SR use |
| trustStorePassword|| Password for truststore |
| keyStorePassword|| Password for keystore used to access SR |

hive.timestamp.format default yyyy-MM-dd HH:mm:ss.SSSSSS,yyyy-MM-dd'T'HH:mm:ss.SSS'Z',yyyy-MM-dd'T'HH:mm:ss.SS'Z',yyyy-MM-dd'T'HH:mm:ss.S'Z',yyyy-MM-dd'T'HH:mm:ss'Z'

Note that the fixed fields include the UUID for the message, the timestamp and message field (a basic string describing the message) and fields (a generic map holding all the fields, flattened).

## TableAPI Writer

TableAPI writer is an optimized version that aims to provide both better performance and ability to have multiple outputs.

At this moment TableAPI writer supports writing to the Hive tables (in the similar way to the default writer) and to other Kafka topics.

To specify where you want the writer to store the output, you need to provide the following property in the index.properties:
```
flink.output-connector
```
Supported values are: 
1. hive - allows storing the data into Hive tables. This Connector is using the main Hive config that's provided for the indexing job;
2. kafka - allows storing the data into other Kafka topics. At this moment we only allow to store output into the same Kafka cluster it's reading from.

Other than specifying the Output Connector you're required to provide the Output Tables init config, and Mappings for those Tables:
```
flink.tables-init-file=/path/to/table-config.json
flink.mapping-file=/path/to/mapping-config.json
```

### Tables Init File
The Tables Init File is used to create Output Tables if those aren't present. For example, you can provide schema of two Tables, but only one of those is missing in the output connectors' Table list, so only the missing one is going to be created.

Example of the Tables Init File contents:

```JSON
{
  "first_table": [
    {
      "name": "full_hostname",
      "type": "string"
    },
    {
      "name": "action",
      "type": "string"
    }
  ],
  "second_hive_table": [
  ]
}
```

In this example you can see two tables specified with only one of those having some column definitions. It's related to the fact that all tables have the same default columns and you can only specify new ones, or override the definition of the default ones.

The default schema is:

```JSON
[
  {
    "name": "id",
    "type": "string"
  },
  {
    "name": "ts",
    "type": "timestamp"
  },
  {
    "name": "fields",
    "type": "map<string,string>"
  },
  {
    "name": "source",
    "type": "string"
  },
  {
    "name": "cyberscore",
    "type": "double"
  },
  {
    "name": "cyberscore_details",
    "type": "array<struct<`ruleid`:string, `score`:double, `reason`:string>>"
  }
]
```

The list of supported column types for all Output Connectors is as follows:
```
string
timestamp
date
int
bigint
float
double
boolean
bytes
null
array<value_type>
map<key_type, value_type>
struct<field_name1:type1, field_name2:type2...>
```

### Mapping File

The Mapping File provides Mapping between the Message objects and Output Tables. Other than providing information how to map fields from the Message to Output Table, it allows to apply transformations to the value and to ignore some fields from the Message Extensions from being carried over to the Output Table.

An example of the Mapping File:
```JSON
{
  "squid": {
    "table_name": "hive_table",
    "ignore_fields": [
      "code"
    ],
    "column_mapping": [
      {
        "name": "full_hostname"
      },
      {
        "name": "action"
      }
    ]
  },
  "test": {
    "table_name": "another_hive_table",
    "column_mapping": [
    ]
  }
}
```

The root key (squid, test) is the Message Source value. So, all Messages with ```source=squid``` are going to be sent to the ```hive_table```, while messages with ```source=test``` are going to be sent into ```another_hive_table```.

The ```ignore_fields``` field allows to specify which Message Extensions to NOT carry over to the ```fields``` column in the output. All other Message Extensions are going to be copied (unless you override the ```fields``` Mapping)

As with the Tables Init File, this config appends to the default mappings:
```JSON
[
  {
    "name": "id",
    "path": "."
  },
  {
    "name": "ts",
    "transformation": "TO_TIMESTAMP_LTZ(%s, 3)",
    "path": "."
  },
  {
    "name": "dt",
    "kafka_name": "ts",
    "transformation": "DATE_FORMAT(TO_TIMESTAMP_LTZ(%s, 3), 'yyyy-MM-dd')",
    "path": "."
  },
  {
    "name": "hr",
    "kafka_name": "ts",
    "transformation": "DATE_FORMAT(TO_TIMESTAMP_LTZ(%s, 3), 'HH')",
    "path": "."
  },
  {
    "name": "source",
    "path": "."
  },
  {
    "name": "cyberscore",
    "kafka_name": "cyberScore",
    "path": ".."
  },
  {
    "name": "cyberscore_details",
    "kafka_name": "cyberScoresDetails",
    "path": ".."
  },
  {
    "name": "fields",
    "kafka_name": "extensions",
    "transformation": "filterMap(%s, %s)",
    "path": "."
  }
]
```

The main Mapping fields are:

| Parameter Name | Description                                                                                                                                                                                                                                                                                                                                                     | Optional | Default Value    |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------|
| name           | Name of the column in the Output Table                                                                                                                                                                                                                                                                                                                          | false    |                  |
| kafka_name     | Name of the Message object field. If left empty, value from the ```name``` is going to be used                                                                                                                                                                                                                                                                  | true     | *same as 'name'* |
| transformation | Transformation that should be applied to the value before storing it to the Output Table. You can call Flink SQL Functions here as well as the custom ones provided below. Inside this field you can have two placeholders (%s). The first one is always replaced with the path(see below), and the second one is replaced with the list of fields to ignore.   | true     |                  |
| path           | Field path relatively to the Message objects' root. By default all values are taken from the Message Extensions. If you need to get data from some other Message field, provide ```.``` in the path. If the Message is a child of some parent object, you can specify the path as ```..```. Think of it as a UNIX path with your work directory in the Message. | true     | ```extensions``` |

For the list of supported Transformation Functions, refer to the [Flink Documentation](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/functions/systemfunctions/) and look for the SQL functions.

Custom functions:

| Function Name                            | Description                                                                                                                                            | Example                                                                                                                                                                                                                                               |
|------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| filterMap(Map<String, String>,String...) | Allows to filter map to not have any keys from the provided list. First argument - map to filter, second argument - list of field names to filter out. | let's say we have Message Extensions (which is a map) with the following keys: ```KeyA```, ```KeyB```, ```KeyC```. If we want to get rid of ```KeyB```, we need to use this function in the following way: ```filterMap(path_to_extensions,"KeyB")``` |
