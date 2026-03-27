package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class AbstractParserOutput {
    private int successfulMessages = 0;
    private int errorMessages = 0;

    public void outputMessage(Message message) {
        List<DataQualityMessage> dqMessages = message.getDataQualityMessages();
        if (dqMessages == null || dqMessages.isEmpty()) {
            successfulMessages++;
        } else {
            errorMessages++;
        }
        output(message);
    }

    protected abstract void output(Message message);
}
