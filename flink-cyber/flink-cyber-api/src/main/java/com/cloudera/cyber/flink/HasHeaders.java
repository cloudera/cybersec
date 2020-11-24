package com.cloudera.cyber.flink;

import java.util.Map;

public interface HasHeaders {
    void setHeaders(Map<String, String> headers);

    Map<String, String> getHeaders();
}
