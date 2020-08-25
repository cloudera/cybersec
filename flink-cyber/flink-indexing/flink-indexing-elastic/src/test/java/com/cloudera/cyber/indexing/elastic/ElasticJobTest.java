package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static org.junit.Assert.fail;

public class ElasticJobTest extends ElasticJob {

    private ManualSource<Message> source;

    @ClassRule
    public ElasticsearchContainer container = new ElasticsearchContainer();


    @BeforeClass
    public void before() {
        container.start();
    }

    @AfterClass
    public void after() {
        container.stop();
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }


    @Test
    public void testSingleDestination() {
        fail("Not implemented");
    }

    @Test
    public void testMultipleDestination() {
        fail("Not implemented");
    }

    @Test
    public void testFieldFiltering() {
        fail("Not implemented");
    }

    @Test
    public void testEventFiltering() {
        fail("Not implemented");
    }
}
