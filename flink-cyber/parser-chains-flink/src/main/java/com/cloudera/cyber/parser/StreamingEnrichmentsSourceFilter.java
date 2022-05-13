package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.FilterFunction;

import java.util.ArrayList;
import java.util.List;

public class StreamingEnrichmentsSourceFilter implements FilterFunction<Message> {
    private final ArrayList<String> streamingEnrichmentSources;

    public StreamingEnrichmentsSourceFilter(List<String> streamingEnrichmentSources) {
        this.streamingEnrichmentSources = new ArrayList<>(streamingEnrichmentSources);
    }

    @Override
    public boolean filter(Message message) {
        return streamingEnrichmentSources.contains(message.getSource());
    }
}
