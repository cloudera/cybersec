package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.EnrichmentEntry;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreatQEntry {
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
    private String tq_url;
    private List<String> tq_tags;

    public static EnrichmentEntry toEnrichmentEntry(ThreatQEntry threatQEntry) {
        return EnrichmentEntry.builder()
                .ts(threatQEntry.tq_updated_at.getTime())
                .key(threatQEntry.getIndicator())
                .type("threatq")
                .entries(new HashMap<String,String>() {{
                    putAll(threatQEntry.getTq_attributes());
                    put("tq_id", threatQEntry.getTq_id().toString());
                    put("tq_status", threatQEntry.getTq_status());
                    put("tq_url", threatQEntry.getTq_url());
                    if (threatQEntry.getTq_tags() != null && threatQEntry.getTq_tags().size() > 0) {
                        put("tq_tags", threatQEntry.getTq_tags().toString());
                    }
                    put("tq_type", threatQEntry.getTq_type());
                    put("tq_saved_search", threatQEntry.getTq_saved_search());
                    if (threatQEntry.getTq_sources() != null && threatQEntry.getTq_sources().size() > 0) {
                        put("tq_sources", threatQEntry.getTq_sources().toString());
                    }
                    put("tq_score", threatQEntry.getTq_score().toString());
                }})
                .build();
    }
}
