package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.ThreatIntelligenceDetails;
import lombok.Builder;
import lombok.Data;
import org.apache.flink.streaming.api.datastream.DataStream;

@Data
@Builder
public class StixResults {
    private DataStream<Message> results;
    private DataStream<ThreatIntelligence> threats;
    private DataStream<ThreatIntelligenceDetails> details;
}
