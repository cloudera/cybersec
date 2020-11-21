package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageRetry {
    private Message message;
    private int retry;
    private long lastTry;
}

