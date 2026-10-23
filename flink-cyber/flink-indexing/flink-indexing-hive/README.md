# Message Indexer

This job takes Message objects from a Kafka Topic and translates them to fit into another kafka topic, a formatted file, or database table.

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
3. iceberg - allows storing the data into Iceberg tables managed by the Hive metastore.  Uses the main Hive config that's provided for the indexing job to find the metastore.
4. filesystem - allows storing the data into json files.  To store as ORC, use the hive Table api writer.

For the filesystem connector, set the following properties:
```
flink.files.path=hdfs:/path/to/written/files
flink.files.format=json
```

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
    "transformation": "filterMap(%s)",
    "path": "."
  }
]
```

The main Mapping fields are:

| Parameter Name | Description                                                                                                                                                                                                                                                                                                                                                     | Optional | Default Value    |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|------------------|
| name           | Name of the column in the Output Table                                                                                                                                                                                                                                                                                                                          | false    |                  |
| kafka_name     | Name of the Message object field. If left empty, value from the ```name``` is going to be used                                                                                                                                                                                                                                                                  | true     | *same as 'name'* |
| transformation | Transformation that should be applied to the value before storing it to the Output Table. You can call Flink SQL Functions here as well as the custom ones provided below. Inside this field you can have placeholders (%s). The first one is always replaced with the path(see below). | true     |                  |
| path           | Field path relatively to the Message objects' root. By default all values are taken from the Message Extensions. If you need to get data from some other Message field, provide ```.``` in the path. If the Message is a child of some parent object, you can specify the path as ```..```. Think of it as a UNIX path with your work directory in the Message. | true     | ```extensions``` |

For the list of supported Transformation Functions, refer to the [Flink Documentation](https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/functions/systemfunctions/) and look for the SQL functions.

Custom functions:

| Function Name                            | Description                                                                                                                                            | Example                                                                                                                                                                                                                                               |
|------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| filterMap(Map<String, String>) | Filters the message extensions map. Each mapping registers its own instance of this function that knows the field names to remove by exact match and the enrichment prefixes to remove by prefix match. The only argument is the map to filter. | For a mapping whose ```fields``` column transformation is ```filterMap(%s)``` with ```kafka_name``` set to ```extensions```, the generated SQL calls the per-mapping function, e.g. ```filterMap<source><table>(message.extensions)```. |

### Schema evolution

Each time indexing starts, the job compares the existing schema of the output table or topic.
1. If the schemas are the same the job starts.
2. If the schema has the same columns but the types are incompatible, the job fails.
3. If the schema has new columns, some connector types can update the schema and others cannot.  If the schema can't be updated, the job fails to start.

| Connector  | New Field Added Behavior on Restart                                                                                                                                                                                     |
|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| kafka      | A new version of the schema is added to the schema registry.  The job creates kafka entries with the new schema.                                                                                                        |
| hive       | The job does not start.  Adding new columns is time consuming.  Manually alter the tables or  create a new table and copy the data from the old table to the new table using default or null values for the new fields. |
| iceberg    | The job alters the table and adds the new columns                                                                                                                                                                       |
| filesystem | The job writes the new fields in the json records.                                                                                                                                                                      |