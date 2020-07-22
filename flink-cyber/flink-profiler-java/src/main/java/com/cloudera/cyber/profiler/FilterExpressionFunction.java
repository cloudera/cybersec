package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.FilterFunction;

public class FilterExpressionFunction implements FilterFunction<Message> {
    public FilterExpressionFunction(String key) {
    }
}
