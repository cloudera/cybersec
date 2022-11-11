# Kafka Utility Commands
## Consume an avro topic and print human json to console
Consume a kafka topic in avro format and convert to json.  Print json to the console.   
### Create properties file
Use the kafka.properties file.  Properties starting with print. prefix will be ignored by other jobs and tool.
Properties starting with kafka. are used by other jobs and tools.

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| schema.registry.url | url | Schema registry rest endpoint url | required | http://myregistryhost:7788/api/v1 |
| kafka.bootstrap.servers | comma separated list | Kafka bootstrap server names and ports. | required | brokerhost1:9092,brokerhost2:9092 |
| kafka.*setting name* | Kafka setting | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html).| set as required by security and performance | |
|print.output.pretty| boolean | if true, pretty print the topic output. if false, print each message json on one line.   | true (pretty multiline output) | false |
|print.max.retries|integer | Number of times kafka consumer will try to consume from the topic. | 5 | 10|
|print.kafka.auto.offset.reset|enum[ealiest, latest, none](https://kafka.apache.org/24/javadoc/constant-values.html#org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG) | Specifies behavior of the consumer when the consumer group is used for the first time. | ealiest | latest |
|print.kafka.group.id|string| Id of consumer group.  | unique consumer id | my_consumer_group|

### Run using java 
```shell script
usr/lib/jvm/java/bin/java  -Dlog4j.configuration=log4j.properties -Dlog4j.configurationFile=log4j.properties -cp kafka-commands-${CYBER_VERSION}.jar:slf4j-api-1.7.15.jar:log4j-1.2-api-2.17.2.jar:log4j-api-2.17.2.jar:log4j-core-2.17.2.jar:log4j-slf4j-impl-2.17.2.jar:/opt/cloudera/parcels/FLINK/lib/flink/lib/flink-dist_2.12-${FLINK_VERSION}.jar  com.cloudera.cyber.kafka.TopicAvroToJson kafka.proerties <topic_name>
```
### Run using devops scripts
```
# cd to directory containing kafka.properties file.
cs-print-topic <topic_name>
```
