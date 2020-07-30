package com.cloudera.cyber.profiler;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
@Builder
public class ProfileConfigGroup {
    private String source;
    private Integer interval;
    private TimeUnit intervalUnit;
    private ProfileMode mode;
    private Long latenessTolerance;

    public static ProfileConfigGroup from(ProfileConfig config) {
        return ProfileConfigGroup.builder()
                .source(config.getSource())
                .interval(config.getInterval())
                .intervalUnit(config.getIntervalUnit())
                .mode(config.getMode())
                .latenessTolerance(config.getLatenessTolerance())
                .build();

    }
}
