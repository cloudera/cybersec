package com.cloudera.parserchains.queryservice.model.describe;

import com.cloudera.parserchains.queryservice.model.sample.ParserSample;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SampleFolderDescriptor {

    private String folderPath;
    List<ParserSample> sampleList;

}
