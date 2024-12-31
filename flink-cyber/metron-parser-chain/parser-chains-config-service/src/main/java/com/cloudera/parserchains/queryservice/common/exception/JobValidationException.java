package com.cloudera.parserchains.queryservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class JobValidationException extends RuntimeException {
    public JobValidationException() {
        this(null,null);
    }
    public JobValidationException(String message) {
        this(message,null);
    }
    public JobValidationException(Throwable cause) {
        this(null,cause);
    }
    public JobValidationException(String message, Throwable cause) {
       super(message);
       if (cause != null) {
           this.initCause(cause);
       }
    }
}
