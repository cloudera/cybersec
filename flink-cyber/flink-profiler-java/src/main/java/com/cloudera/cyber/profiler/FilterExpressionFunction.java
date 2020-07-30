package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.RichFilterFunction;

public class FilterExpressionFunction extends RichFilterFunction<Message> implements FilterFunction<Message> {
    private final String filterExpr;

    public FilterExpressionFunction(String filterExpr) {
        super();
        this.filterExpr = filterExpr;
    }

    @Override
    public boolean filter(Message message) throws Exception {
        // implement a filter based on the where clause style in the expression
        return false;
    }
}
