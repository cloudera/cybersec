package com.cloudera.parserchains.queryservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class KafkaException extends RuntimeException {
    public KafkaException() {
        this(null,null);
    }
    public KafkaException(String message) {
        this(message,null);
    }
    public KafkaException(Throwable cause) {
        this(null,cause);
    }
    public KafkaException(String message, Throwable cause) {
       super(message);
       if (cause != null) {
           this.initCause(cause);
       }
    }
}
