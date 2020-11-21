package com.cloudera.cyber.enrichment.threatq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThreatQConfig implements Serializable {
    private String field;
    private String indicatorType;
}
