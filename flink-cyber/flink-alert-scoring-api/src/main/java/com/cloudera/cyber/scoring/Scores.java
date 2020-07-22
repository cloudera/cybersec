package com.cloudera.cyber.scoring;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Scores {
    private UUID ruleId;
    private Double score;
    private String reason;

}
