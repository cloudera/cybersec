package com.cloudera.parserchains.queryservice.handler;

import com.cloudera.parserchains.queryservice.controller.impl.DefaultParserController;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParserHandler {

  private final DefaultParserController parserController;

  public ResponseEntity<?> findAll() throws IOException {
    return parserController.findAll(null);
  }

  public ResponseEntity<?> describeAll() throws IOException {
    return parserController.describeAll(null);
  }

}
