# ThreatQ integration

This process handles loading, parsing and storing ThreatQ events to Hbase. It also applies the theatq data to 
messages in Enrichment.

The process requires an HBase table called `threatq` with a single cf `t`.

Keys are of the form `indicator_type:indicator`

All fields in the CF are added with a prefix.

Configuration is of the form: 

```json
[
    { "field": "ip_src_addr", "indicatorType": "IP Address" },
    { "field": "ip_dst_addr", "indicatorType": "IP Address" }
]
```

Settings for the standalone job are in a properties file.

TODO


Setup: 

hbase shell

create 'threatq', 't'

