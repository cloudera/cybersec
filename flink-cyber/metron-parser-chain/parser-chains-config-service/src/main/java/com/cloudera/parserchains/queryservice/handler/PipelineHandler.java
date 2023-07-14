package com.cloudera.parserchains.queryservice.handler;

import com.cloudera.parserchains.queryservice.controller.impl.DefaultPipelineController;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PipelineHandler {

  private final DefaultPipelineController pipelineController;

  public ResponseEntity<?> findAll() throws IOException {
    return pipelineController.findAll(null);
  }

}
