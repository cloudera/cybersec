package com.cloudera.cyber.scoring;

import com.cloudera.cyber.IdentifiedMessage;
import com.cloudera.cyber.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class ScoredMessage implements IdentifiedMessage {
    private Message message;
    private List<Scores> scores;
    private List<ScoringRule> rules;

    @Override
    public String getId() {
        return message.getId();
    }

    @Override
    public long getTs() {
        return message.getTs().getMillis();
    }
}
