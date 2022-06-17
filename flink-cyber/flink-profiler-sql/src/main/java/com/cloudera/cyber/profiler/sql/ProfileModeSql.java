package com.cloudera.cyber.profiler.sql;

import static com.cloudera.cyber.flink.Utils.readResourceFile;

public enum ProfileModeSql {
    TUMBLING("sql/profile.sql"),
    SLIDING("sql/profile_sliding.sql");

    private final String sqlText;

    private ProfileModeSql(String fileName) {
        sqlText = readResourceFile(fileName, ProfileModeSql.class);
    }

    public String getSqlText() {
        return sqlText;
    }
}
