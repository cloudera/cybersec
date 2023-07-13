## Enrichment Configuration
The Enrichment Configuration defines how enrichments are stored and the key and value fields required by the enrichment.  Enrichment loading and lookup use the Enrichment Configuration to write and read the enrichments.

## Enrichment Storage Formats

### Metron Format
The Metron format allows legacy Metron enrichment tables to be consumed by cybersec without migration.  Metron formatted enrichments perform better at scale but they are not human readable.
 EnrichmentKey equals the key field value or the key field values joined with the delimiter.  

| HBase Rowkey | Column Family | Column Qualifier | Value |
| -------------| ------------| -------------------| ------|
| 128bit hash + EnrichmentType + EnrichmentKey |Specified column family|v| Enrichment values as a Map |

The example scan below shows an enrichment of type 'majestic_million' with key field 'wordpress.com' and value {'rank' = '21}

```shell script
hbase:003:0> scan 'metron_enrich'
ROW                                                      COLUMN+CELL                                                                                                                                                        
 \x04+\x83C\xEEt==\x1D\x95\x0Bk\xE9i\x81\x11\x00\x10maje column=cf:v, timestamp=1652187778829, value={"rank":"21"}                                                                                                          
 stic_million\x00\x0Dwordpress.com                                                                                                                                                                                          
```

### Simple Format
The Simple format is more human readable but less efficient.   Each enrichment type maps to a separate column family.
 EnrichmentKey equals the key field value or the key field values joined with the delimiter.  

| HBase Rowkey | Column Family | Column Qualifier | Value |
| -------------| ------------| -------------------| ------|
| EnrichmentKey | EnrichmentType | FieldName      | FieldValue |
 
The example scan below shows an enrichment of type 'malicious_domain' with key field '039b1ee.netsolhost.com' and value {'source' = 'abuse.ch' }

```shell script
hbase:006:0> scan 'simple_enrich'
ROW                                                      COLUMN+CELL                                                                                                                                                        
 039b1ee.netsolhost.com                                  column=id:key, timestamp=1651779191837, value=039b1ee.netsolhost.com                                                                                               
 039b1ee.netsolhost.com                                  column=malicious_domain:source, timestamp=1651779191837, value=abuse.ch                                                                                            
```
### Loading enrichments
The keyFields and valueFields of the [EnrichmentFieldsConfig](#enrichmentfieldsconfig-json) define how to construct an enrichment from a source.   Supported sources include CSV files (batch) or parsed events (streaming).
#### Batch
Use the flink-enrichment-loading batch job to load enrichments from CSV files.   
#### Streaming
If an enrichment defines one or more streamingSources, the parser writes the enrichment when it produces a source message matching one of the streamingSources. 

### Example Minimal Config
The example below shows a single enrichment table stored in Metron format.  The malicious_domain enrichment specifies only the keyFields.  When converting a source to malicious_domain, the value includes all fields in the source except the key field (domain).
The majestic_million enrichment defines values fields.  When converting a source to majestic_million, the value includes only the rank field. 

```json
{
  "storageConfigs": {
    "default": {
      "format": "HBASE_METRON",
      "hbaseTableName": "metron_enrichments",
      "columnFamily": "cf"
    }
  },
  "enrichmentConfigs": {
    "malicious_domain": {
      "fieldMapping": {
        "keyFields": [
          "domain"
        ]
      }
    },
    "majestic_million": {
      "fieldMapping": {
        "keyFields": [
          "domain"
        ],
        "valueFields": [
          "rank"
        ]
      }
    }
  }
}
```

### Override Default Storage
The example below stores enrichments in multiple formats and defines streaming sources.
The malicious_domain enrichment is in simple format in the 'simple_enrichments' hbase table with an 'id' and a 'malicious_domain' column family to store the malicious_domain enrichments.
The malicious_domain enrichment can be created by the parser from a structured message with source 'malicious_domain'.
The majestic_million enrichment is in the default format.  The default is Metron format in the 'metron_enrichments' hbase table with a 'cf' column family.   
The majestic_million enrichment is ingested in batch only.
```json
{
  "storageConfigs": {
    "default": {
      "format": "HBASE_METRON",
      "hbaseTableName": "metron_enrichments",
      "columnFamily": "cf"
    },
    "simple": {
      "format": "HBASE_SIMPLE",
      "hbaseTableName": "simple_enrichments"
    }
  },
  "enrichmentConfigs": {
    "malicious_domain": {
      "storage": "simple",
      "fieldMapping": {
        "keyFields": [
          "domain"
        ],
        "valueFields": [
          "source"
        ],
        "streamingSources": [
          "malicious_domain"
        ]
      }
    },
    "majestic_million": {
      "storage": null,
      "fieldMapping": {
        "keyFields": [
          "domain"
        ],
        "valueFields": [
          "rank"
        ]
      }
    }
  }
}
```

### Reserved Enrichment Types
There are two reserved enrichment names: threatq and first_seen.   Reserved enrichments define a mapping to the hbase table, column family and storage format.
The keys and values are defined by the enrichment and profile jobs.  

* The first_seen enrichment mapping is ignored.  Specify the hbase table and format using the profile properties file settings.  

* If there is no specific mapping for threatq enrichment, the default table and format is used.

* To store threatq enrichments in a different table and format, override the threatq enrichment.  In the example below the threatq enrichment is store in the threatq table 
and cf column family with the HBASE_METRON format.
```
{
  "storageConfigs": {
    "default": {
      "format": "HBASE_METRON",
      "hbaseTableName": "enrich_default",
      "columnFamily": "cf"
    },
    "threatq": {
      "format": "HBASE_METRON",
      "hbaseTableName": "threatq",
      "columnFamily": "cf"
    }
  },
  "enrichmentConfigs": {
    "threatq" : {
       "storage": "threatq",
       "fieldMapping": {
       }
    }
}
```

## EnrichmentsConfig Json

| Json Field       | Type                              | Description                | Required/Default | 
| -----------------| --------                          | -------------------------- |  ----------------|
| storageConfigs   | Map storage name to EnrichmentStorageConfig | Defines storage formats used by enrichment types.  | must contain at least one storage format named "default".  Use the default storage format when no format is specified.   |
| enrichmentConfigs  | Map enrichment type name to EnrichmentConfig   | Defines how to extract enrichment types from sources and how to store the enrichments in Hbase.  | required | 

### Validation Errors
| Error | Meaning |
| ------| --------|
| Null or empty string are not valid storageTypeNames | The name in the map can't be empty or null.  It must be a unique string. |
| Could not deserialize enrichments configuration file '\<file name\>' | The json syntax is incorrect or the json does not match the EnrichmentsConfigFile schema |
| Enrichment storage does not contain configuration for default. | The storageConfigs section must contain an entry named default |
 
## EnrichmentStorageConfig Json

| Json Field       | Type                              | Description                | Required/Default | Example |
| -----------------| --------                          | -------------------------- |  ----------------| --------|
| format   | enum { HBASE_METRON, HBASE_SIMPLE } | Defines how to convert enrichments keys and values to the storage key and value.  | required | HBASE_METRON |
| hbaseTableName | String | The name of the hbase table storing the enrichment | Required when stored in Hbase |  enrichments |
| columnFamily | String | The name of the hbase column family for HBASE_METRON enrichments. | Required when stored in HBASE_METRON format | cf |

### Validation Errors
| Error | Meaning |
| ------| --------|
| EnrichmentStorageConfig <storage_name>: Hbase table name must be set and non-empty| HBASE_METRON and HBASE_SIMPLE formats require an hbase table name.|
| EnrichmentStorageConfig <storage_name>: Hbase metron enrichment format requires column family| Add a columnFamily field to the enrichment storage.   HBASE_METRON format requires a column family.|
| EnrichmentStorageConfig <storage_name>: Hbase simple enrichment format does not require a column family| Remove the columnFamily field to the enrichment storage.   HBASE_SIMPLE format should not define a column family.|

## EnrichmentConfig Json

| Json Field       | Type                              | Description                | Required/Default | Example |
| -----------------| --------                          | -------------------------- |  ----------------| --------|
| storage          | String  | Name of the [EnrichmentStorageConfig](#enrichmentstorageconfig-json) for the enrichment. | if null, use "default" EnrichmentStorageConfig | storage_config_name |
| fieldMapping     | [EnrichmentFieldsConfig](#enrichmentfieldsconfig-json) | Specifies the key fields, value fields, and streaming sources of the enrichment. | required | |

### Validation Errors
| Error | Meaning |
| ------| --------|
|EnrichmentConfig <enrichment_type_name>: references undefined storage <storage_name>.| There is no entry in storageConfigs called <storage_name>.  Add a storage configuration or change to an existing storage.  Set to null to use the default. |
|EnrichmentConfig <enrichment_type_name>, field mapping not specified| The EnrichmentConfig requires a fieldMapping setting.  Add fieldMapping to the EnrichmentConfig json.|

## EnrichmentFieldsConfig Json

| Json Field       | Type                              | Description                | Required/Default | Example |
| -----------------| --------                          | -------------------------- |  ----------------| --------|
| keyFields          | array of Strings | Names of the key fields for the enrichment type.  The values of the fields concatenated must specify a unique key. | required | ["key_field_name"] |
| keyDelimiter          | Strings | For enrichments types with multiple key fields.   Place the delimiter between each key field value. | ":" | - |
| valueFields     | array of Strings | Names of the value fields. | optional.  if unspecified use all non-key fields | ["value_field_name"] |
| streamingSources     | array of Strings | When the parser reads a message with one of the sources, write an enrichment of this type. | optional - if unspecified, enrichment is loaded in batch only. | ["majestic_million"]|

### Validation Errors
| Error | Meaning |
| ------| --------|
|EnrichmentFieldsConfig <enrichment_type>: EnrichmentConfig.keyFields is null or empty.  Must contain at least one field name| Add an array of keyFields with at least one keyField name.|
|EnrichmentFieldsConfig <enrichment_type>: EnrichmentConfig.keyFields has duplicate values.  All key field names must be unique.| Remove or modify duplicate names in the keyFields.|
|EnrichmentFieldsConfig <enrichment_type>:EnrichmentConfig.valueFields is empty.  Must be null or list of fields| Remove the valueFields to add all non-key fields to the enrichment value or add at least one field name to the array to add specific enrichment values.|
|EnrichmentFieldsConfig <enrichment_type>: EnrichmentConfig.valueFields has duplicate values.  All value field names must be unique.|Remove or modify duplicate names in the valueFields.|

