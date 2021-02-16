package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.functions.FilterFunction;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessageSourceFilter implements FilterFunction<Message> {
    private List<String> sources;

    @Override
    public boolean filter(Message message) {
        return sources.contains(message.getSource());
    }
}
