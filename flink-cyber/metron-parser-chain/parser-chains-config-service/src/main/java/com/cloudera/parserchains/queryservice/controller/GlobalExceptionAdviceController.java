package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.service.common.response.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionAdviceController {

    @ExceptionHandler(FailedClusterReponseException.class)
    protected ResponseEntity<ResponseBody> handleFailedClusterRequest(FailedClusterReponseException ex) {
        return new ResponseEntity<>(ex.getResponseBody(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FailedAllClusterReponseException.class)
    protected ResponseEntity<List<ResponseBody>> handleFailedAllClusterRequest(FailedAllClusterReponseException ex) {
        return new ResponseEntity<>(ex.getResponseBodies(), HttpStatus.BAD_REQUEST);
    }
}
