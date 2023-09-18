package com.cloudera.parserchains.queryservice.common.exception;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
@StandardException
public class FailedJobAction extends RuntimeException {
}
