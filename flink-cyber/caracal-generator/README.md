# Raw Event Generator 

The event generator creates raw events on a topic when real data is not available during testing or demonstration.  

The generator creates new events by injecting random or preconfigured parameters into [Freemarker](https://freemarker.apache.org/) templates. The weight defines the number of times the template is selected during generation.   The higher the weight, the more messages the generated topic will contain messages from that template. 

## Event Generation
The event generator has three different modes:
1. [Generate preconfigured text to preconfigured topics.](#generating-preconfigured-text-events-to-preconfigured-topics)
2. [Generate preconfigured avro events to preconfigured topic.](#generating-preconfigured-avro-events-to-preconfigured-topic)
3. [Generate custom text and/or avro events on specified topics.](#generating-custom-text-and-avro-events-on-specified-topics)

### Generating preconfigured text events to preconfigured topics
To generate the preconfigured topics, use the following properties:
```
generator.eps=20
```
The generator creates the following preconfigured topics:

| Topic | Format | Template | Weight |
|--------|-------|------------|--------|
| netflow | JSON Text | [netflow_1](./src/main/resources/Netflow/netflow_sample_1.json) | 2.0 |
|  | JSON Text | [netflow_2](./src/main/resources/Netflow/netflow_sample_2.json) | 4.0 |
|  | JSON Text | [netflow_3](./src/main/resources/Netflow/netflow_sample_2.json) | 1.0 |
| netflow_b |JSON Text |  [netflow_b](./src/main/resources/Netflow/netflow_sample_b.json) | 1.0 |
|  | JSON Text | [netflow_b_error](./src/main/resources/Netflow/netflow_sample_b_error.json) | 1.0 |
| dpi_http | JSON Text | [http_1](./src/main/resources/DPI_Logs/Metadata_Module/http/http_sample_1.json) |1.5|
|  | JSON Text | [http_2](./src/main/resources/DPI_Logs/Metadata_Module/http/http_sample_2.json) |1.0|
|  | JSON Text | [http_3](./src/main/resources/DPI_Logs/Metadata_Module/http/http_sample_3.json) |1.0|
|  | JSON Text | [http_4](./src/main/resources/DPI_Logs/Metadata_Module/http/http_sample_4.json) |1.0|
| dpi_dns | JSON Text | [dns_1](./src/main/resources/DPI_Logs/Metadata_Module/DNS/dns_sample_1.json)|1.0|
|  | JSON Text | [dns_2](./src/main/resources/DPI_Logs/Metadata_Module/DNS/dns_sample_2.json)|1.0|
|  | JSON Text | [dns_3](./src/main/resources/DPI_Logs/Metadata_Module/DNS/dns_sample_3.json)|1.0|
| dpi_smtp |JSON Text | [smtp_1](./src/main/resources/DPI_Logs/Metadata_Module/SMTP/smtp_sample_1.json) |1.0|
| threats | JSON Text | [threats](./src/main/resources/threats/threatq.json)|1.0|

The generator selects ip addresses included in the topics output and when creating the threatq threat intelligence entries in the threats topic. 

### Generating preconfigured avro events to preconfigured topic
To generate the preconfigured avro events to topics, use the following properties:
```
generator.eps=20
generator.avro.flag=true
```

The generator creates the following preconfigured topics:

| Topic | Format | Template | Weight |
|--------|-------|----------|--------|
| generator.avro | Avro [netflow](./src/main/resources/Netflow/netflow.schema)|[netflow_b](./src/main/resources/Netflow/netflow_avro_sample1.json) | 1.0 |

The generator does not create any threatq entries in this configuration. 

### Generating custom text and avro events on specified topics
To generate custom avro and text events to custom topics, specify a generator config:

```
generator.eps=20
generator.config=config/generator_config.json
```

The generator config file below generates the following:
1. squid text topic with randomly selected source and destination ip addresses
2. squid_scenario text topic with specific combinations of source and ip addresses
3. netflow_bb avro topic with randomly selected ip addresses.
4. netflow_bb8 avro topic with specific combinations of source and ip addresses 

```
{
  "baseDirectory": "config",
  "generationSources" :
    [
      {
        "file": "squid.txt",
        "topic": "squid",
        "outputAvroSchemaFile": null,
        "weight": 1.0
      },
      {
        "file": "squid_scenario.txt",
        "topic": "squid_scenario",
        "outputAvroSchemaFile": null,
        "weight": 1.0,
        "scenarioFile": "parameters.csv"
      },
      {
        "file": "Netflow/netflow_avro_sample1.json",
        "topic": "netflow_bb",
        "outputAvroSchemaFile": "Netflow/netflow.schema",
        "weight": 1.0
      },
      {
        "file": "netflow_bb8_template.json",
        "topic": "netflow_bb8",
        "outputAvroSchemaFile": "netflow.schema",
        "weight": 1.0,
        "scenarioFile": "parameters.csv"
      }
  ]
}
```

The squid.txt file template uses the utils.randomIP and utils.randomInt macros to populate the template with random data:
```
${((ts)/1000)?c}  ${utils.randomInt(1024,65535)?c} ${utils.randomIP()} TCP_TUNNEL/200 414 CONNECT www.google.com:443 - HIER_DIRECT/${utils.randomIP()} -
```
The squid_scenario.txt file template uses the params.<scenario parameter name> to inject specific combinations of values into the generated events:
```
${((ts)/1000)?c}  ${utils.randomInt(1024,65535)?c} ${params.ip_src_addr} ${params.action}/${params.code} 414 CONNECT ${params.url} - HIER_DIRECT/${params.ip_dst_addr} -
```
The parameters.csv file defines the combinations of values for the params macro.   The file is in CSV format.  The first line is a header that defines the parameter names.  The following lines are the values of the parameters:
```
ip_src_addr,url,ip_dst_addr,action,code
165.1.200.190,www.google.com:443,142.250.191.164,TCP_TUNNEL,200
165.1.200.191,http://cnn.com/,151.101.3.5,TCP_MISS,301
```
# Error Handling
Generator detects the following error conditions and will not start:
1. The generator config file is not legal json.
2. The generator.avro.flag is true and generator.config is specified.

# Configuration Properties

## General Properties Configuration

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| parallelism | integer | Number of parallel tasks to run.  | default=2 | 2 |
| checkpoint.interval.ms | integer | Milliseconds between Flink state checkpoints | default=60000 | 10000|
| schema.registry.url | url | Schema registry rest endpoint url | required | http://myregistryhost:7788/api/v1 |
| kafka.bootstrap.servers | comma separated list | Kafka bootstrap server names and ports. | required | brokerhost1:9092,brokerhost2:9092 |
| kafka.*setting name* | Kafka setting | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html).| set as required by security and performance | |
| flink.job.name | string | Set the Flink job name as it will appear in the Flink dashboard. | Triaging Job - default | my_pipeline.triage |

## Generation properties

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| generator.count | Integer > 0 | Maximum number of events to generate. | generates events continuously with no upper limite | 100 |
| generator.eps | Integer > 0 | Events per second to generate for each template. | 0 | 10|
| generator.metrics | topic name | Write number of events written to each topic per time period to this topic. | generator.metrics | events.generated |
| generator.config | file name | File specifying which topics to generate and the format of each topic.  | no default - generates preconfigured topics | generator_config.json |

# Running the Generator Job
* Construct a 'generator.properties' file using the configuration options above.

```
# general job
flink.job.name=my-pipeline.parser_name.generator
parallelism=1

# kafka and schema registry 
kafka.bootstrap.servers=cybersec-1.vpc.cloudera.com:9092,cybersec-1.vpc.cloudera.com:9092
kafka.acks=all
kafka.client.id=my-pipeline-triage
kafka.group.id=my-pipeline-triage
schema.registry.url=http://cybersec-1.vpc.cloudera.com:7788/api/v1

# generator
generator.eps=20

```
*  Run the generator job using the flink run command.  If using customer configuration, use the -yt command to ship configuration directory to the yarn job.

```
flink run \
-yt generator_config \
 --jobmanager yarn-cluster -yjm 2048 -ytm 2048  --detached --yarnname "my_pipeline.parser_name.generator" caracal-generator-2.3.0.jar generator.properties
```