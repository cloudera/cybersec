package com.cloudera.parserchains.queryservice.common.exception;

import com.cloudera.service.common.response.ResponseBody;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FailedAllClusterReponseException extends Exception {

    private final transient List<ResponseBody> responseBodies;
}
