package com.cloudera.parserchains.queryservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FailedJobAction extends RuntimeException {
    public FailedJobAction() {
        this(null,null);
    }
    public FailedJobAction(String message) {
        this(message,null);
    }
    public FailedJobAction(Throwable cause) {
        this(null,cause);
    }
    public FailedJobAction(String message, Throwable cause) {
       super(message);
       if (cause != null) {
           this.initCause(cause);
       }
    }
}
