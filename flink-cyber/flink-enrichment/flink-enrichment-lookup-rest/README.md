# Lookup Rest Enrichment 

This enrichment constructs a REST HTTP request, runs the rest call and then adds the properties returned as message extensions.  
Rest enrichments can augment messages with the model predictions from a CML/CDSW model or organization specific information such as personnel or asset information.
Rest enrichments are expensive and should be applied sparingly.  

## Properties Configuration

| Property Name | Type                                    | Description                                  | Required/Default |Example             |
|---------------| ----------------------------------------| -------------------------------------------- | ------------------- | -----------------|
| config.file    | File path  | Json file containing a list of rest enrichments to apply to messages.  See [section below](#rest-enrichment-configurations) for details | Required | enrichment-rest.json |
| schema.registry.url | url | Schema registry rest endpoint url | required | http://myregistryhost:7788/api/v1 |
| topic.input | topic name | Incoming messages to be enriched.  Stored in AVRO Message format managed by schema registry. | required | enrichment.input |
| topic.output | topic name | Outgoing enriched messages.  Stored in AVRO message format managed by schema registry. | required | enrichment.output |
| parallelism | integer | Number of parallel tasks to run.  | default=2 | 2 |
| checkpoint.interval.ms | integer | Milliseconds between Flink state checkpoints | default=60000 | 10000|
| kafka.bootstrap.servers | comma separated list | Kafka bootstrap server names and ports. | required | brokerhost1:9092,brokerhost2:9092 |
| kafka.*setting name* | Kafka setting | Settings for [Kafka producers](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/ProducerConfig.html) or [Kafka consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html).| set as required by security and performance | |

### Example properties file
```
config.file=enrichment-rest.json
geo.database_path=hdfs:/user/centos/flink-cyber/geo/GeoLite2-City.mmdb
topic.input=enrichment.input
topic.output=enrichment.geo

kafka.bootstrap.servers=<kafka-bootstrap>
schema.registry.url=http://<schema-registry-server>:7788/api/v1
```

## Rest Enrichment Configurations
Specify how to apply rest enrichments to the extensions of a message in the rest enrichment JSON config file.   The json file consists of a list of rest configurations with the fields below.  
The Flink job calls each Rest service sequentially for each message.  If the rest enrichment is not relevant for a particular message the message passes on immediately to the next stage.
Rest services execute asynchronously and unordered.

### RestEnrichmentConfiguration Json Fields

| Json Field       | Type                              | Description                                                                                                           | Required/Default | Example                                |
| -----------------| --------                          | --------------------------                                                                                            | ---------------- | -------------------------------------- |
| endpointTemplate | [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html) template String | For GET or POST requests, Rest url template with config and extension variable substitutions.  Must evaluate to a valid URI after substitution. | Required         | "${protocol}://${server}/asset?id=${id}" |
| entityTemplate   | [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html) template String | For GET requests, entity template with config and extension variables substitutions.                                  | Required for POST | "{\"accessKey\":\"${access_key}\",\"request\":{\"domain\":\"${domain}\"}}" |   
| method           | enum (GET,POST)                   | HTTP method required by the REST interface                                                                            | GET               | "POST" |
| tls              | TLS trust and keystore            | If server uses https with mutual authentication, specify the keystore and trust store jks. See fields below.          | null              | see below                             | 
| sources          | string list                       | Enrich only events with the specified sources.  Use ANY to enrich all events that define the extensions required in the endpointTemplate and entityTemplate | required | \[ "dns", "squid" \] or \[ "ANY" \] |
| authorization    | Endpoint authorization            | Authorization required by the rest service.   Basic and Bearer Token auth are supported.  To supply other types of authorization headers, use the headers field. | No default | see below |
| headers          | Map of header name to header value template   | Optional headers required by rest service such as content type.  Values may contain references to properties  | Optional.        |   "headers" : { "Content-Type" : "application/json" } | 
| properties       | Map of property name to string value | Properties to substitute in endpointTemplate, entityTemplate, resultJsonPath and statusJsonPath.                    | null             |   "properties" : { "protocol" : "https" } |
| timeoutMillis    | Long Milliseconds Duration           | Number of milliseconds to wait for the Flink asynchronous operation to complete. Adjust based on response speed of the rest endpoint. | 1000 ms          | 3000 |
| capacity         | Integer                           | Number of asynchronous requests executing at once | 1000 | 500 |
| cacheSize        | Integer                           | Number of rest results to cache.  The cache will evict expired or least recently used results when it reaches capacity. | 1000            | 1500             |
| successCacheExpirationSeconds | Long Seconds               | Seconds before a successful cache result is refreshed.  A successful result returns a 2xx HTTP status and has a true success indicator.  | 1800 seconds (30 minutes) | 300 |
| failureCacheExpirationSeconds | Long Seconds               | Seconds before an unsuccessful cache result is refreshed.  An unsuccessful result returns a 4xx or 5xx HTTP status or has a false success indicator | 300 seconds (5 minutes) | 300 |
| prefix           | String | Prefix to prepend to the extensions returned by the rest service.  For example if the prefix is "dga_model" and the rest service returns a field called "legit", add an extension called "dga_model.legit" to the enriched event. | Required | model_name |
| successJsonPath  | [Jsonpath](https://github.com/json-path/JsonPath) String | The json path for extracting the success or failure boolean from the entity returned by the rest service.  If set, the result is success if the HTTP response code is 2xx and the success code specified by the json path is true.   If not set, the HTTP response code determines the success or failure of the rest service. | null | "$\['success'\]" | 
| resultsJsonPath  | [Jsonpath](https://github.com/json-path/JsonPath) String | The json path for extracting the extensions from the entity returned by the rest service. | $ | "$\['response'\]" | 

## TLS trust and keystore configuration

Specify the TLS connection information when the rest service uses https with mutual authentication. 

| Json Field       | Type                              | Description                                                                                                           | Required/Default | Example                                |
| ----------------- | --------                          | --------------------------                                                                                            | ---------------- | -------------------------------------- |
| trustStorePath    | File Path may be local file or hdfs | The path to the JKS file containing the trusted CA certificates.                                               | Required         | /opt/rest/config/truststore.jks   |
| trustStorePassword | String                           | Password for decrypting the trust store.                                                                              | Required         | trustpassword                          |
| keyStorePath      | File Path may be local file or hdfs | The path to the JKS file containing the client identity certificate.                                                | Required         | /opt/rest/config/keystore.jks   |
| keyStorePassword  | String                            | Password for decrypting the key store.                                                                                | Required          | keypassword                  |
| keyAlias          | String                            | If keystore contains multiple keys, the alias of the client identity.                                                 | Optional          | keyalias |
| keyPassword       | String                            | The key password if it differs from the keystore password.  Otherwise, use null.                                      | null              | keypassword |

## Authorization configuration

If the REST service requires authorization, specify the options below.  The options below construct the Authorization headers for basic (user/password) and bearer token authorization.   
To specify other types of authorization, add the Authorization header name and value to the headers section of the rest enrichment configuration.

### Basic Authorization

Adds standard Authorization header with value of \'Authorization: Basic \<base64 encoded username:password\>\'

| Json Field       | Type                              | Description                                                                                                           | Required/Default | Example                                |
| ----------------- | --------                          | --------------------------                                                                                            | ---------------- | -------------------------------------- |
| type              | enum(basic)          | The type of authorization required by the rest server.                                                               | Required        | basic |
| userNameTemplate  |  [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html) template String | Template for user name.  May use property variables. | Required by basic | myusername |
| passwordTemplate  |  [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html) template String | Template for password.  May use property variables. | Required by basic | mypassword |

### Token Authorization

Adds Authorization header with value \'Bearer \<bearer token\>\'

| Json Field       | Type                              | Description                                                                                                           | Required/Default | Example                                |
| ----------------- | --------                          | --------------------------                                                                                            | ---------------- | -------------------------------------- |
| type              | enum (token)                | The type of authorization required by the rest server.                                                               | Required        | token |
| bearerTokenTemplate |  [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html) template String | Template for bearer token.  May use property variables. | Required by token | mybearertokenaae6b840d045b574d96e35e271419720d0d7645534da6d5ba3d.74c9e8867ef7e0750b5772671acf7e413a744f6d77507eac83584014c71c5866 |

## Sample Rest Enrichment Configuration 
** Example messages are shown in json for readability.  Actual messages will be formatted in AVRO **

### Call CML or CDSW Model with Authentication Enabled
For all messages with source dns, make a POST rest call to the model deployed in CML with Enable Authentication checked.  Substitute the protocol and server properties into the endpoint url. 
Substitute the access_key property and domain message extension in the entity.   For example if the message is :
 ```json
   {
        "source": "dns",
        "domain": "google.com"
   }
 ```
The job constructs the following REST request:
```
Method: POST
Headers: 
Content-Type: application/json
Authorization: Bearer mybearertokenaae6b840d045b574d96e35e271419720d0d7645534da6d5ba3d.74c9e8867ef7e0750b5772671acf7e413a744f6d77507eac83584014c71c5866
Url: https://modelservice.ml-2f4cffbb-91e.demo-aws.ylcu-atmi.mycompany.site/model
Entity: {"accessKey":"myaccesskeymup8kz1hsl3erczwepbt8","request":{"domain":"google.com"}}
```

The model returns a json document with a success flag and the legit response.
```json
   {
        "success": true,
        "response": {
            "legit": true
        }
   }
```  
The enriched message has a new extension dga_model.legit set to true.
```json
  {
        "source": "dns",
        "domain": "google.com",
        "dga_model.legit": true
  }
```
Example rest enrichment configuration to call the CDSW or CML model:
```json
[ {
  "endpointTemplate" : "${protocol}://${server}/model",
  "entityTemplate" : "{\"accessKey\":\"${access_key}\",\"request\":{\"domain\":\"${domain}\"}}",
  "method" : "POST",
  "tls" : null,
  "sources" : [ "dns" ],
  "authorization" : {
    "type" : "token",
    "bearerTokenTemplate" : "${bearer_token}"
  },
  "headers" : {
    "Content-Type" : "application/json"
  },
  "properties" : {
    "server" : "modelservice.ml-2f4cffbb-91e.demo-aws.ylcu-atmi.mycompany.site",
    "bearer_token" : "mybearertokenaae6b840d045b574d96e35e271419720d0d7645534da6d5ba3d.74c9e8867ef7e0750b5772671acf7e413a744f6d77507eac83584014c71c5866",
    "protocol" : "https",
    "access_key" : "myaccesskeymup8kz1hsl3erczwepbt8"
  },
  "prefix" : "dga_model",
  "successJsonPath" : "$['success']",
  "resultsJsonPath" : "$['response']"
}]
```

### Make a GET request with TLS mutual auth
For all messages with an extension named id, make a GET rest call using TLS mutual auth.  Substitute the protocol property, the server property, and message id extension into the endpoint url. 
For example if the message is:

```json
   {
        "source": "scanresult",
        "id": "1234"
   }
 ```

The job constructs the following REST request:
```
Method: GET
Headers: 
Authorization: Basic YWxhZGRpbjpvcGVuc2VzYW1l
Url: https://resthost/asset?id=1234
```

The model returns a json document with the location = US property.
```json
   {
         "location": "US"
   }
```  
The enriched message has a new extension asset_info.location set to US.
```json
   {
        "source": "scanresult",
        "id": "1234",
        "asset_info.location": "US"
   }
 ```

Example rest enrichment configuration to call the asset location service:

```json
[{
  "endpointTemplate" : "${protocol}://${server}/asset?id=${id}",
  "entityTemplate" : null,
  "method" : "GET",
  "tls" : {
    "trustStorePath" : "src/test/resources/config/cert/test-truststore.jks",
    "trustStorePassword" : "password",
    "keyStorePath" : "src/test/resources/config/cert/test-client.jks",
    "keyStorePassword" : "password",
    "keyAlias" : null,
    "keyPassword" : "keypass"
  },
  "sources" : [ "ANY" ],
  "authorization" : {
    "type" : "basic",
    "userNameTemplate" : "${user}",
    "passwordTemplate" : "${password}"
  },
  "headers" : { },
  "properties" : {
    "server" : "resthost",
    "password" : "opensesame",
    "protocol" : "https",
    "user" : "aladdin"
  },
  "timeoutMillis" : 1000,
  "capacity" : 3,
  "cacheSize" : 10000,
  "successCacheExpirationSeconds" : 1800,
  "failureCacheExpirationSeconds" : 300,
  "prefix" : "asset_info",
  "successJsonPath" : null,
  "resultsJsonPath" : "$"
}]
```
  
## Data Quality Messages
The rest enrichment reports the following messages. 
 
| Severity Level | Feature | Message | Meaning |
| ---------------| ------- | ------- | ------- |
| ERROR           | rest     | Rest request url=\'\<endpoint url\>\' entity=\'\<entity string\>\' failed \'Rest request returned a success code but the content indicated failure.' | The successJsonPath is specified, but the boolean was false |
| ERROR           | rest     | Rest request url=\'\<endpoint url\>\' entity=\'\<entity string\>\' failed 'Rest request failed due to \'\<failed HTTP status message\>\'.\' | The rest request returned a 4xx or 5xx status. |
| ERROR           | rest     | Variable(s) \'\<variable name\>\' required by rest url are undefined | Source was not ANY and the endpoint url contains an undefined property or extension.  |
| ERROR           | rest     | Variable(s) \'\<variable name\>\' required by rest entity are undefined | Source was not ANY and the entity url contains an undefined property or extension. |
| ERROR           | rest     | Rest URI \'\<invalid rest endpoint url\>\' is invalid. | The endpoint url is invalid because it has an incorrect format or unreachable host name. |

## Running the job

```
flink run -Dlog4j.configurationFile=enrichment-rest-log4j.properties --jobmanager yarn-cluster -yjm 1024 -ytm 1024 --detached --yarnname "EnrichmentRest" flink-enrichment-lookup-rest-0.0.1-SNAPSHOT.jar enrichment-rest.properties
```