# IP Geocode Enrichment 

This enrichment reads the values of one or more specified message extensions containing either an ip string or a collection of ip strings
and produces a new message augmented with the geolocations of each extension.   The ip geocoding information is provided by the [Maxmind GeoIP2 City Database](https://www.maxmind.com/en/geoip2-city) or by the [IPinfo geolocation Database](https://ipinfo.io/data/ip-geolocation).   

| Source extension value type    | Produced extension             | Description                                                                                                                                        |
|--------------------------------|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| Ipv4 or Ipv6 String            | <extension_name>.geo.city      | (String) Name of city where ip is located.  Omitted if Maxmind database does not specify city.                                                     |
|                                | <extension_name>.geo.state     | (String) Subdivision or region (state, county, province, prefecture) of the ip address.  Omitted if Maxmind database does not specify subdivision. |
|                                | <extension_name>.geo.country   | (String) Code of country for the ip address.  Omitted if Maximind database does not specify country.                                               |
|                                | <extension_name>.geo.latitude  | (Float) Latitude of the ip address.  Omitted if Maximind database does not specify latitude.                                                       |
|                                | <extension_name>.geo.longitude | (Float) Longitude of the ip address.  Omitted if Maximind database does not specify longitude.                                                     |
| Ipv4 or Ipv6 String Collection | <extension_name>.geo.cities    | (String Set) Names of cities where ips are located.  Omitted if no ips in the list have a city in the Maxmind database.                            |
|                                | <extension_name>.geo.countries | (String Set) Codes of countries where ips are located.  Omitted if no ips in the list have a country in the Maxmind database.                      |
## Data Quality Messages
The geocode enrichment mapping reports the following messages. 
 
| Severity Level | Feature | Message                                                 |
|----------------|---------|---------------------------------------------------------|
| INFO           | geo     | 'extension value or element' is not a String.           |
| INFO           | geo     | 'extension value or element' is not a valid IP address. |
| ERROR          | geo     | Geocode failed 'reason'                                 |
### Example
** Examples are shown in json for readability.  Actual messages will be formatted in AVRO **
#### original message
```json
  {
       "dst_ip": "210.204.98.208"
  }
```

#### enriched message after geo applied to dst_ip extension
```json
  {
       "dst_ip": "210.204.98.208",
       "dst_ip.geo.longitude": 126.9741,
       "dst_ip.geo.latitude": 37.5112,
       "dst_ip.geo.country": "KR"
  }
```

# IP ASN Enrichment
This enrichment reads the values of one or more specified message extensions containing an ip string
and produces a new message augmented with the Autonomous System Numbers(ASN) and Organizations.   ASNs map an external IP to an organization and provides
addition information for [threat hunting and detection](https://www.huntress.com/blog/utilizing-asns-for-hunting-and-response). 
 The ip ASN information is provided by the [Maxmind GeoLite ASN Database](https://dev.maxmind.com/geoip/docs/databases/asn/) or by the [IPinfo ASN Database](https://ipinfo.io/developers/asn-database).

| Source extension value type | Produced extension          | Description                                                                                                                                                           |
|-----------------------------|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Ipv4 or Ipv6 String         | <extension_name>.asn.org    | (String) Name of Organization hosting the Autonomous System Number.  Omitted if the database does not contain an organization name.  Example: "Cloudflare, Inc."      |
|                             | <extension_name>.asn.mask   | (String) The CIDR for the IP addresses routed by the organization.  Omitted if the database does not have information about the IP address. Example: "152.114.0.0/17" |
|                             | <extension_name>.asn.number | (String) The Autonomous System Number for the IP address.  Omittied if the database does not have information about the IP address. Example: "13335"                  |

## Data Quality Messages
The geocode enrichment mapping reports the following messages.

| Severity Level | Feature | Message                                                 |
|----------------|---------|---------------------------------------------------------|
| INFO           | asn     | 'extension value or element' is not a String.           |
| INFO           | asn     | 'extension value or element' is not a valid IP address. |
| ERROR          | asn     | ASN lookup failed '%reason'                             |
### Example
** Examples are shown in json for readability.  Actual messages will be formatted in AVRO **
#### original message
```json
  {
       "dst_ip": "1.0.4.0"
  }
```

#### enriched message after asn applied to dst_ip extension
````json
  {
       "dst_ip": "1.0.4.0",
       "dst_ip.asn.org": "Gtelecom Pty Ltd",
       "dst_ip.asn.mask": "1.0.4.0/22",
       "dst_ip.asn.number": "38803"
  }
````
# IP Company Enrichment
Company enrichment reads the values of one or more specified message extensions containing an IP string
and produces a new message augmented with the company and ASN hosting that IP. 
The ip company information is provided only by [IPinfo IP to Company Database](https://ipinfo.io/developers/ip-to-company-database).

| Source extension value type | Produced extension                  | Description                                                                                                                                                                      |
|-----------------------------|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Ipv4 or Ipv6 String         | <extension_name>.company.asn.org    | (String) Name of Organization hosting the Autonomous System Number for the compnay.  Omitted if the database does not contain an organization name.  Example: "Cloudflare, Inc." |
|                             | <extension_name>.company.name       | (String) Name of the company for the IP address.  Omitted i the database does not have information about the IP address. Example: "i2ts,inc."                                    |
|                             | <extension_name>.company.mask       | (String) The CIDR for the IP addresses routed by the company.  Omitted if the database does not have information about the IP address. Example: "152.114.0.0/17"                 |
|                             | <extension_name>.company.asn.number | (String) The Autonomous System Number for the IP address.  Omittied if the database does not have information about the IP address. Example: "13335"                             |

## Data Quality Messages
The geocode enrichment mapping reports the following messages.

| Severity Level | Feature | Message                                                 |
|----------------|---------|---------------------------------------------------------|
| INFO           | company | 'extension value or element' is not a String.           |
| INFO           | company | 'extension value or element' is not a valid IP address. |
| ERROR          | company | Company lookup failed '%reason'                         |
### Example
** Examples are shown in json for readability.  Actual messages will be formatted in AVRO **
#### original message
```json
  {
       "dst_ip": "1.0.16.0"
  }
```

#### enriched message after company applied to dst_ip extension
````json
  {
       "dst_ip": "1.0.16.0",
       "dst_ip.company.name": "i2ts,inc.",
       "dst_ip.company.mask": "1.0.16.0/24", 
       "dst_ip.company.asn.org": "ARTERIA Networks Corporation",
       "dst_ip.company.asn.number": "2519"
   }
````

# Configuration

| Property Name           | Type                                    | Description                                                                                                                                                                                                                                                    | Required/Default                            | Example                                               |
|-------------------------|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|-------------------------------------------------------|
| geo.enabled             | boolean                                 | If true, apply geocode enrichments to geo.ip_fields.                                                                                                                                                                                                           | default=true                                | false                                                 |
| geo.ip_fields           | comma separated list of extension names | Apply geocode enrichment to these extensions                                                                                                                                                                                                                   | required if geo.enabled=true                | ip_dst,ip_src                                         |
| geo.database_path       | hdfs or local file system uri           | Location of the Maxmind or IPinfo Geolocation .mmdb file.   Supports .mmdb, mmdb.gz, and .tar.gz files.                                                                                                                                                        | required if geo.enabled=true                | hdfs:/user/myuser/flink-cyber/geo/GeoLite2-City.mmdb  |
| asn.enabled             | boolean                                 | If true, apply asn enrichments to ip_fields.                                                                                                                                                                                                                   | default=true                                | false                                                 |
| asn.ip_fields           | comma separated list of extension names | Apply asn enrichment to these extensions                                                                                                                                                                                                                       | required if asn.enabled=true                | ip_dst,ip_src                                         |
| asn.database_path       | hdfs or local file system uri           | Location of the Maxmind or IPinfo ASN .mmdb file.   Supports .mmdb, mmdb.gz, and .tar.gz files.                                                                                                                                                                | required if asn.enabled=true                | hdfs:/user/myuser/flink-cyber/geo/GeoLite2-ASN.mmdb   |
| company.enabled         | boolean                                 | If true, apply company enrichments to ip_fields.                                                                                                                                                                                                               | default=true                                | false                                                 |
| company.ip_fields       | comma separated list of extension names | Apply company enrichment to these extensions                                                                                                                                                                                                                   | required if company.enabled=true            | ip_dst,ip_src                                         |
| company.database_path   | hdfs or local file system uri           | Location of the Ipinfo company .mmdb file.   Supports .mmdb, mmdb.gz, and .tar.gz files.                                                                                                                                                                       | required if company.enabled=true            | hdfs:/user/myuser/flink-cyber/geo/ipinfo_company.mmdb |
| schema.registry.url     | url                                     | Schema registry rest endpoint url                                                                                                                                                                                                                              | required                                    | https://myregistryhost:7790/api/v1                    |
| topic.input             | topic name                              | Incoming messages to be enriched.  Stored in AVRO Message format managed by schema registry.                                                                                                                                                                   | required                                    | enrichment.input                                      |
| topic.output            | topic name                              | Outgoing enriched messages.  Stored in AVRO message format managed by schema registry.                                                                                                                                                                         | required                                    | enrichment.output                                     |
| parallelism             | integer                                 | Number of parallel tasks to run.                                                                                                                                                                                                                               | default=2                                   | 2                                                     |
| checkpoint.interval.ms  | integer                                 | Milliseconds between Flink state checkpoints                                                                                                                                                                                                                   | default=60000                               | 10000                                                 |
| kafka.bootstrap.servers | comma separated list                    | Kafka bootstrap server names and ports.                                                                                                                                                                                                                        | required                                    | brokerhost1:9092,brokerhost2:9092                     |
| kafka.*setting name*    | Kafka setting                           | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html). | set as required by security and performance |                                                       |

## Example properties file
```
geo.enabled=true
geo.ip_fields=ip_src_addr,ip_dst_addr,ip_dst,ip_src
geo.database_path=hdfs:/user/cybersec/cybersec/example/reference/geo/ipinfo_plus_sample.mmdb

asn.enabled=true
asn.ip_fields=ip_src_addr,ip_dst_addr,ip_dst,ip_src
asn.database_path=hdfs:/user/cybersec/cybersec/example/reference/geo/ip_asn_sample.mmdb

company.enabled=true
company.ip_fields=ip_src_addr,ip_dst_addr,ip_dst,ip_src
company.database_path=hdfs:/user/cybersec/cybersec/example/reference/geo/ip_company_sample.mmdb

topic.input=enrichment.input
topic.output=enrichment.geo

kafka.bootstrap.servers=<kafka-bootstrap>
schema.registry.url=https://<schema-registry-server>:7790/api/v1
```

# Running the job

In production the enrichment job is included in the Combined Enrichment job. 

For testing purposes, the geo job can be run in isolation as below.

```
flink run -Dlog4j.configurationFile=enrichment-geo-log4j.properties --jobmanager yarn-cluster -yjm 1024 -ytm 1024 --detached --yarnname "EnrichmentGeo" flink-enrichment-geocode-0.0.1-SNAPSHOT.jar enrichment-geo.properties
```