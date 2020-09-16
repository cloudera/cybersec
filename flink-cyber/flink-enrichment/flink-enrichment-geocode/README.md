# IP Geocode Enrichment 

This enrichment reads the values of one or more specified message extensions containing either an ip string or a collection of ip strings
and produces a new message augmented with the geolocations of each extension.   The ip geocoding information is provided by the [Maxmind GeoIP2 City Database](https://www.maxmind.com/en/geoip2-city).   

| Source extension value type | Produced extension    | Description |
| --------------------------  |--------------------------------| ------------|
| Ipv4 or Ipv6 String         | <extension_name>.geo.city      | (String) Name of city where ip is located.  Omitted if Maxmind database does not specify city. |
|                             | <extension_name>.geo.state     | (String) Subdivision or region (state, county, province, prefecture) of the ip address.  Omitted if Maxmind database does not specify subdivision.|
|                             | <extension_name>.geo.country   | (String) Code of country for the ip address.  Omitted if Maximind database does not specify country. |
|                             | <extension_name>.geo.latitude  | (Float) Latitude of the ip address.  Omitted if Maximind database does not specify latitude.|
|                             | <extension_name>.geo.longitude | (Float) Longitude of the ip address.  Omitted if Maximind database does not specify longitude.|
| Ipv4 or Ipv6 String Collection  | <extension_name>.geo.cities    | (String Set) Names of cities where ips are located.  Omitted if no ips in the list have a city in the Maxmind database. |
|                             | <extension_name>.geo.countries | (String Set) Codes of countries where ips are located.  Omitted if no ips in the list have a country in the Maxmind database. |
## Data Quality Messages
The geocode enrichment mapping reports the following messages. 
 
| Severity Level | Feature | Message |
| ---------------| ------- | ------- |
| INFO           | geo     | 'extension value or element' is not a String. |
| INFO           | geo     | 'extension value or element' is not a valid IP address. |
| ERROR          | geo     | Geocode failed 'reason' |
### Example
** Examples are shown in json for readability.  Actual messages will be formatted in AVRO **
#### original message
```json
  {
       "dst_ip": "210.204.98.208"
  }
```

#### enriched message after geocode applied to dst_ip extension
```json
  {
       "dst_ip": "210.204.98.208",
       "dst_ip.geo.longitude": 126.9741,
       "dst_ip.geo.latitude": 37.5112,
       "dst_ip.geo.country": "KR"
  }
```

## Configuration

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| geo.ip_fields | comma separated list of extension names | Apply geocode enrichment to these extensions | required | ip_dst,ip_src       |
| geo.database_path | hdfs or local file system uri       | Location of the Maxmind GeoIP2 city database .mmdb file.   [Register for a Maxmind account](https://www.maxmind.com/en/geolite2/signup)  Download the binary .mmdb file from the accounts page of the web UI or set up a script to [download directly](https://dev.maxmind.com/geoip/geoip-direct-downloads/).  Gunzip and tar xvf the download.  If running in a yarn cluster, use the hdfs command line (hdfs dfs -put or hdfs dfs -moveFromLocal to store the database directory including the mmdb file in HDFS.| required | hdfs:/user/myuser/flink-cyber/geo/GeoLite2-City.mmdb |
| schema.registry.url | url | Schema registry rest endpoint url | required | http://myregistryhost:7788/api/v1 |
| topic.input | topic name | Incoming messages to be enriched.  Stored in AVRO Message format managed by schema registry. | required | enrichment.input |
| topic.output | topic name | Outgoing enriched messages.  Stored in AVRO message format managed by schema registry. | required | enrichment.output |
| parallelism | integer | Number of parallel tasks to run.  | default=2 | 2 |
| checkpoint.interval.ms | integer | Milliseconds between Flink state checkpoints | default=60000 | 10000|
| kafka.bootstrap.servers | comma separated list | Kafka bootstrap server names and ports. | required | brokerhost1:9092,brokerhost2:9092 |
| kafka.*setting name* | Kafka setting | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html).| set as required by security and performance | |

### Example properties file
```
geo.ip_fields=dst_ip
geo.database_path=hdfs:/user/centos/flink-cyber/geo/GeoLite2-City.mmdb
topic.input=enrichment.input
topic.output=enrichment.geo

kafka.bootstrap.servers=<kafka-bootstrap>
schema.registry.url=http://<schema-registry-server>:7788/api/v1
```

## Running the job

```
flink run -Dlog4j.configurationFile=enrichment-geo-log4j.properties --jobmanager yarn-cluster -yjm 1024 -ytm 1024 --detached --yarnname "EnrichmentGeo" flink-enrichment-geocode-0.0.1-SNAPSHOT.jar enrichment-geo.properties
```