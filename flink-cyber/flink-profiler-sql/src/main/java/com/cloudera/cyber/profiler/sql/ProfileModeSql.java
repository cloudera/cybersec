package com.cloudera.cyber.profiler.sql;

import static com.cloudera.cyber.flink.Utils.readResourceFile;

public enum ProfileModeSql extends ProfileModer {
    TUMBLING("sql/profile.sql"),
    SLIDING("sql/profile_sliding.sql");

    private final String sqlText;

    private ProfileMode(String fileName) {
        sqlText = readResourceFile(fileName, ProfileMode.class);
    }

    public String getSqlText() {
        return sqlText;
    }
}
