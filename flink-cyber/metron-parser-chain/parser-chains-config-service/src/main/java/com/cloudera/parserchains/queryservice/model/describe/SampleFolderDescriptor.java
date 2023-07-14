package com.cloudera.parserchains.queryservice.model.describe;

import com.cloudera.parserchains.queryservice.model.sample.ParserSample;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SampleFolderDescriptor {

    private String folderPath;
    List<ParserSample> sampleList;

}
