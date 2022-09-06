# Enrichment Config File
The enrichment config file defines what enrichments to apply to each message source.  It is a text file in json format.

## EnrichmentsConfig Json

| Json Field       | Type                              | Description                | Required/Default | 
| -----------------| --------                          | -------------------------- |  ----------------|
| source   | string | Source of message to enrich.  Apply these enrichments to the specified source.   | required   |
| kind  | enum [LOCAL,HBASE]   | Defines where the enrichment data is stored.  | required | 
| fields | list of EnrichmentFields | Maps message fields to enrichments | required |

### EnrichmentFields

| Json Field       | Type                              | Description                | Required/Default | 
| -----------------| --------                          | -------------------------- |  ----------------|
| name  | string | Use this field value as the key to the enrichment. | required |
| enrichmentType | string | Name of the enrichment type to apply.  | required |

## Example

In the example below, the triaging job will apply the following enrichments to messages with squid source:
1. Lookup domain in the malicious_domain map stored in Flink state.
2. Lookup domain in the domain_category map stored in Flink state.
3. Lookup domain in the majestic_million mapping stored in HBase.

```json
[
  {
    "source": "squid",
    "kind": "LOCAL",
    "fields": [
      {
        "name": "domain",
        "enrichmentType": "malicious_domain"
      },
      {
        "name": "domain",
        "enrichmentType": "domain_category"
      }
    ]
  },
  {
    "source": "squid",
    "kind": "HBASE",
    "fields": [
      {
        "name": "domain",
        "enrichmentType": "majestic_million"
      }
    ]
  }
]
```
