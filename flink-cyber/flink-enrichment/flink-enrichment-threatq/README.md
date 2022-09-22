# ThreatQ Threat Intelligence

This job loads, parses, and stores ThreatQ threat intelligence indicators in Hbase. It also applies the threatq indicators to 
messages.

The process requires an HBase table called `threatq` with a single cf `t`.

Keys are of the form `indicator_type:indicator`

All fields in the column family are added with a prefix.

## Ingesting threatq enrichments
* Create the hbase table to store threatq enrichments.
```
hbase shell
create 'threatq', 't'
```

* Export threatq threat intelligence in json format into the threatq enrichment topic of the triaging flink job.
```
{'indicator': '0.42.42.42', 'tq_sources': ['TQ 2 TQ'], 'tq_created_at': '2022-09-05 11:39:09', 'tq_score': '0', 'tq_type': 'IP Address', 'tq_saved_search': 'ip_search', 'tq_updated_at': '2022-09-05 11:39:09', 'tq_touched_at': '2022-09-05 11:39:09', 'tq_id': 89022, 'tq_attributes': {'Confidence': 'High', 'Severity': 'Unknown', 'Reference': 'https://investigate.umbrella.com/ip-view/91.189.114.7', 'Share': 'Yes', 'Added to Infoblox RPZ': 'threatqrpz', 'Priority': '90', 'Disposition': 'Unknown', 'CTR Module': 'Umbrella'}, 'tq_status': 'Active', 'tq_tags': None, 'tq_url': 'https://10.13.0.159/indicators/89022/details'}
```
* The flink triaging job ingests the threat intelligence and stores it threatq hbase table.  The HBase key is `<tq_type>:<indicator>`
```
hbase:004:0> get 'threatq', 'IP Address:0.42.42.42'
COLUMN                                                CELL                                                                                                                                                       
 t:Added to Infoblox RPZ                              timestamp=1662429241368, value=threatqrpz                                                                                                                  
 t:CTR Module                                         timestamp=1662429241368, value=Umbrella                                                                                                                    
 t:Confidence                                         timestamp=1662429241368, value=High                                                                                                                        
 t:Disposition                                        timestamp=1662429241368, value=Unknown                                                                                                                     
 t:Priority                                           timestamp=1662429241368, value=90                                                                                                                          
 t:Reference                                          timestamp=1662429241368, value=https://investigate.umbrella.com/ip-view/91.189.114.7                                                                       
 t:Severity                                           timestamp=1662429241368, value=Unknown                                                                                                                     
 t:Share                                              timestamp=1662429241368, value=Yes                                                                                                                         
 t:createdAt                                          timestamp=1662429241368, value=\xFF\xFF\xC7\xC7\xD5/3H                                                                                                     
 t:id                                                 timestamp=1662429241368, value=89022                                                                                                                       
 t:savedSearch                                        timestamp=1662429241368, value=ip_search                                                                                                                   
 t:score                                              timestamp=1662429241368, value=\x00\x00\x00\x00                                                                                                            
 t:sources                                            timestamp=1662429241368, value=TQ 2 TQ                                                                                                                     
 t:status                                             timestamp=1662429241368, value=Active                                                                                                                      
 t:touchedAt                                          timestamp=1662429241368, value=\xFF\xFF\xC7\xC7\xD5/3H                                                                                                     
 t:type                                               timestamp=1662429241368, value=IP Address                                                                                                                  
 t:updatedAt                                          timestamp=1662429241368, value=\xFF\xFF\xC7\xC7\xD5/3H                                                                                                     
 t:url                                                timestamp=1662429241368, value=https://10.13.0.159/indicators/89022/details 
```
## Enriching messages with Threatq Threat Intelligence
* Create a threatq.json configuration file.  Map the fields in the events to the threatq indicators to be matched.
In the example below.   The enrichment job looks for threatq IP Address indicators for the ip_src_addr and ip_dst_addr message fields.
```json
[
    { "field": "ip_src_addr", "indicatorType": "IP Address" },
    { "field": "ip_dst_addr", "indicatorType": "IP Address" }
]
```
* When the triaging job finds a match on threatq indicator, it adds fields to the message named <source_field>.threatq.<threatq_indicator_attribute>  For exmaple:

```
        "ip_dst_addr":"0.42.42.42",
        "ip_dst_addr.threatq.touchedAt":"-61813887651000",
         "ip_dst_addr.threatq.url":"https://10.13.0.159/indicators/89022/details",
         "ip_dst_addr.threatq.updatedAt":"-61813887651000",
         "ip_dst_addr.threatq.type":"IP Address",
         "ip_dst_addr.threatq.Disposition":"Unknown",
          "ip_dst_addr.threatq.Severity":"Unknown",
         "ip_dst_addr.threatq.score":"0.0",
          "ip_dst_addr.threatq.savedSearch":"ip_search",
         "ip_dst_addr.threatq.status":"Active",
         "ip_dst_addr.threatq.Added to Infoblox RPZ":"threatqrpz",
         "ip_src_addr":"10.19.8.116",
         "ip_dst_addr.threatq.id":"89022",
         "ip_dst_addr.threatq.createdAt":"-61813887651000",
         "ip_dst_addr.threatq.Reference":"https://investigate.umbrella.com/ip-view/91.189.114.7",
          "ip_dst_addr.threatq.CTR Module":"Umbrella",
         "ip_dst_addr.threatq.Confidence":"High",
         "ip_dst_addr.threatq.Priority":"90",
         "ip_dst_addr.threatq.sources":"TQ 2 TQ",
```
* To alert on Threatq matches, add a scoring rule that returns a non-zero score when the messages contains a threatq enrichment field.  For example the rule below 
returns a score of 90 if the ip_src_addr field matches an IP Address indicator.
```
{
  "name": "ThreatQ Intel Match",
  "id" : "47a7759f-7b5a-456a-b064-a605a4633080",
  "order" : "1",
  "tsStart" : "2020-01-01T00:00:00.00Z",
  "tsEnd" : "2025-01-01T00:00:00.00Z",
  "type" : "JS",
  "ruleScript" : "if (message.containsKey(\"ip_dst_addr.threatq.type\")) {return {score: 90.0, reason: 'ThreatQ intel match'};} else { return {score: 0.0, reason: 'no match'}};",
  "enabled" : true
}
```
