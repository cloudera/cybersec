package com.cloudera.cyber.profiler;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.java.functions.KeySelector;

import java.util.List;
import com.cloudera.cyber.Message;

import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class MessageKeySelector implements KeySelector<Message, String>{

    private List<String> fieldNames;

    public MessageKeySelector(List<String> fieldNames) {
        Preconditions.checkNotNull(fieldNames, "profile key field list is null");
        Preconditions.checkArgument(!fieldNames.isEmpty(),"profiled key field list must contain at least one field name");
        this.fieldNames = fieldNames;
    }

    @Override
    public String getKey(Message message) {
        Map<String, String> extensions = message.getExtensions();
        return fieldNames.stream().map(extensions::get).collect(Collectors.joining("-"));
    }
}