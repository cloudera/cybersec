package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.functions.FilterFunction;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class MessageFieldFilter implements FilterFunction<Message> {
    protected static final String NULL_MESSAGE_FIELD_LIST_ERROR = "profiled message field list is null";
    protected static final String EMPTY_MESSAGE_FIELD_LIST_ERROR = "profiled message field list must contain at least one field name";
    private List<String> fieldNames;

    public MessageFieldFilter(List<String> fieldNames) {
        Preconditions.checkNotNull(fieldNames, NULL_MESSAGE_FIELD_LIST_ERROR);
        Preconditions.checkArgument(!fieldNames.isEmpty(), EMPTY_MESSAGE_FIELD_LIST_ERROR);
        this.fieldNames = fieldNames;
    }

    @Override
    public boolean filter(Message message) {
        Map<String, String> extensions = message.getExtensions();
        return fieldNames.stream().allMatch(extensions::containsKey);
    }
}
