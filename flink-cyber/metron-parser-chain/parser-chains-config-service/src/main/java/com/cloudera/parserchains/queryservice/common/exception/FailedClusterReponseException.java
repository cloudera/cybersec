package com.cloudera.parserchains.queryservice.common.exception;

import com.cloudera.service.common.response.ResponseBody;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FailedClusterReponseException extends Exception {

    private final transient ResponseBody responseBody;
}
