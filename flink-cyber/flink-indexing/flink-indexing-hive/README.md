# Hive Indexer

This job takes Message objects from a Kafka Topic and translates them to fit into a Hive Table.

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
