package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class MessageRetry {
    private Message message;
    private int retry;
    private long lastTry;
}

