package com.cloudera.cyber.enrichment.threatq;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThreatQEntry {
    /*
    {'indicator': '91.189.114.7', 'tq_sources': ['TQ 2 TQ'], 'tq_created_at': '2020-10-16 15:19:37', 'tq_score': '0', 'tq_type': 'IP Address', 'tq_saved_search': 'ip_search', 'tq_updated_at': '2020-10-16 15:19:37', 'tq_touched_at': '2020-11-03 17:58:38', 'tq_id': 89022, 'tq_attributes': {'Confidence': 'High', 'Severity': 'Unknown', 'Reference': 'https://investigate.umbrella.com/ip-view/91.189.114.7', 'Share': 'Yes', 'Added to Infoblox RPZ': 'threatqrpz', 'Priority': '90', 'Disposition': 'Unknown', 'CTR Module': 'Umbrella'}, 'tq_status': 'Active', 'tq_tags': None, 'tq_url': 'https://10.13.0.159/indicators/89022/details'}
     */
    private String indicator;
    private List<String> tq_sources;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date tq_created_at;
    private Float tq_score;
    private String tq_type;
    private String tq_saved_search;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date tq_updated_at;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date tq_touched_at;

    private Long tq_id;
    private Map<String, String> tq_attributes;
    private String tq_status;
    private List<String> tq_tags;

}
