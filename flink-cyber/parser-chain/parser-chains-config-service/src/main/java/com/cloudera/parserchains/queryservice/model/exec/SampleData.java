package com.cloudera.parserchains.queryservice.model.exec;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the sample data that is received by the front-end
 * when a test execution of a parser chain is requested.
 *
 *  <p>See also {@link ChainTestRequest} which is the top-level class for the
 *  data model used for the "Live View" feature.
 */
public class SampleData {

    /**
     * The type of sample data, which by design could be "kafka", "hdfs" or "manual".  In
     * reality, this will only every be manual as Kafka and HDFS are not currently supported.
     */
    private String type;

    /**
     * Contains the sample data that should be parsed by the parser chain.
     */
    private List<String> source;

    public SampleData() {
        this.source = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public SampleData setType(String type) {
        this.type = type;
        return this;
    }

    public List<String> getSource() {
        return source;
    }

    public void setSource(List<String> source) {
        this.source = source;
    }

    public SampleData addSource(String toParse) {
        this.source.add(toParse);
        return this;
    }
}
