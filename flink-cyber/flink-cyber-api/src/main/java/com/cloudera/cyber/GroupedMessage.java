package com.cloudera.cyber;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class GroupedMessage implements IdentifiedMessage {
    protected UUID id;
    protected List<Message> messages;

    private Long _ts;
    private Long _startTs;

    public Long getStartTs() {
        if (_startTs == null)
            _startTs = getMessages().stream().map(Message::getTs).min(Long::compareTo).get();
        return _startTs;
    }
    public Long getTs() {
        if (_ts == null) _ts = getMessages().stream().map(Message::getTs).max(Long::compareTo).get();
        return _ts;
    }

    public void setMessages(List<Message> message) {
        this.messages = messages;
        this._ts = null;
        this._startTs = null;
    }

    public String toString() {
        return String.format("GroupedMessage(id=%s, startTs=%d, ts=%d, messages=%s", getId(), getStartTs(), getTs(), getMessages());
    }
}
