package com.cloudera.parserchains.queryservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class KafkaClusterNotFound extends RuntimeException {
    public KafkaClusterNotFound() {
        this(null, null);
    }

    public KafkaClusterNotFound(String message) {
        this(message, null);
    }

    public KafkaClusterNotFound(Throwable cause) {
        this(null, cause);
    }

    public KafkaClusterNotFound(String message, Throwable cause) {
        super(message);
        if (cause != null) {
            this.initCause(cause);
        }
    }
}
