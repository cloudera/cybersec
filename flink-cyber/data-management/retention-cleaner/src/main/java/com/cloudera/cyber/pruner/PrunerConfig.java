package com.cloudera.cyber.pruner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrunerConfig {

    @Builder.Default
    private String originalLocation = "/data/originals/";
    @Builder.Default
    private String logsLocation = "/data/logs/";

    /**
     * Hive Table containing all the events
     */
    @Builder.Default
    private String eventsTable = "events";

    @Builder.Default
    private long originalsMaxAge = 30;
    @Builder.Default
    private long logsMaxAge = 30;
    @Builder.Default
    private long eventsMaxAge = 30;

    @Builder.Default
    private String datePattern = "yyyy-MM-dd--HH";

    private transient DateFormat dateFormat;

    public DateFormat getDateFormatter() {
        if (dateFormat == null)
            this.dateFormat = new SimpleDateFormat(datePattern);
        return dateFormat;
    }

    public long getOriginalsMaxMs() {
        return originalsMaxAge * 3600000;
    }

    public long getLogsMaxMs() {
        return logsMaxAge * 3600000;
    }
    public long getEventsMaxMs() {
        return eventsMaxAge * 3600000;
    }
}
