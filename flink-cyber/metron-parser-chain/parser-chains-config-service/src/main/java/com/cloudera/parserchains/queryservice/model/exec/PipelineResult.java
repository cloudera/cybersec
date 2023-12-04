package com.cloudera.parserchains.queryservice.model.exec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.core.fs.Path;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineResult {

  private String name;
  private Path path;

}
