# Hive Indexer

This job takes Message objects from a Kafka Topic and translates them to fit into a Hive Table.

The process reads the structure of the hive table and extracts the fields required to populate it from the message. It also expects a minimum set of fields in the hive table, and a column which accepts a map to contain any non-extracted fields.

For example, the following hive table schema could be used as a destination if you are using the partitioning sink mode:
```SQL
SET hive.txn.manager=org.apache.hadoop.hive.ql.lockmgr.DbTxnManager;

CREATE TABLE `events`(                    
   `id` string,                                     
   `ts` bigint,                                  
   `message` string,                                
   `fields` map<string,string>,                     
   `ip_src_addr` string,                            
   `ip_dst_addr` string,                            
   `ip_src_port` string,                          
   `ip_dst_port` string)
PARTITIONED BY (source string, dt string, hr string) 
STORED AS ORC
TBLPROPERTIES (
  'partition.time-extractor.timestamp-pattern'='$dt $hr:00:00',
  'sink.partition-commit.trigger'='partition-time',
  'sink.partition-commit.delay'='30 s',
  'sink.partition-commit.policy.kind'='metastore,success-file',
  'transactional'='true',
  'transactional_properties'='insert_only'
);

```

There is also a mode which uses Hive Streaming.

| parameter | default | explanation |
| --- | --- | --- |
| hive.table | events | Table to publish messages to |
| hive.catalog | default | A catalog to store events in |
| hive.schema | cyber | The name of the database in Hive |
|||
| topic.input || Source topic in Kafka |
| kafka.*|| Ability to override Kafka Consumer settings |
| schema.registry.url|http://<schema-regsitry-host>:7788/api/v1| location of schema registry|
| schema.registry.client.ssl|| whether to use ssl for SR |
| trustStorePath|| Location of truststore for SR use |
| trustStorePassword|| Password for truststore |
| keyStorePassword|| Password for keystore used to access SR |

Note that the fixed fields include the UUID for the mesage, the timestamp and message field (a basic string describing the message) and fields (a generic map holding all the fields, flattened).
