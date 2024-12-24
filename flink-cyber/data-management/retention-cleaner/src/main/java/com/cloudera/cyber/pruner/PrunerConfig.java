/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.pruner;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * Hive Table containing all the events.
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
        if (dateFormat == null) {
            this.dateFormat = new SimpleDateFormat(datePattern);
        }
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
