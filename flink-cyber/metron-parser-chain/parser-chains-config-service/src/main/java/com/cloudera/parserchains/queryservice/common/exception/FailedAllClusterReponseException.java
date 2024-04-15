package com.cloudera.parserchains.queryservice.common.exception;

import com.cloudera.service.common.response.ResponseBody;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class FailedAllClusterReponseException extends Exception {

    private final transient List<ResponseBody> responseBodies;
}
