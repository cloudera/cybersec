package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.FilterFunction;

public class FilterProfiles implements FilterFunction<Message> {
    public FilterProfiles(String filter) {
    }
}
