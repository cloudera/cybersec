package com.cloudera.parserchains.queryservice.common.exception;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
@StandardException
public class KafkaClusterNotFound extends RuntimeException {
}
