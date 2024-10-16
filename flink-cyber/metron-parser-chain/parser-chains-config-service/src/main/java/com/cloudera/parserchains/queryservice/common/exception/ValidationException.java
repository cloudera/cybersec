package com.cloudera.parserchains.queryservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {
    public ValidationException() {
        this(null,null);
    }
    public ValidationException(String message) {
        this(message,null);
    }
    public ValidationException(Throwable cause) {
        this(null,cause);
    }
    public ValidationException(String message, Throwable cause) {
       super(message);
       if (cause != null) {
           this.initCause(cause);
       }
    }
}
