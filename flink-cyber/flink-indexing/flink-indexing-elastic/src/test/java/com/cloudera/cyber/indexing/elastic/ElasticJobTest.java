package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.indexing.CollectionField;
import com.cloudera.cyber.indexing.MessageRetry;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.*;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;

import static org.junit.Assert.fail;

public class ElasticJobTest extends ElasticJob {

    private ManualSource<Message> source;
    private ManualSource<CollectionField> configSource;

    @Rule
    public ElasticsearchContainer container = new ElasticsearchContainer();

    @Before
    public void before() {
        container.start();
    }

    @After
    public void after() {
        container.stop();
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }

    @Override
    protected DataStream<CollectionField> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        configSource = JobTester.createManualSource(env, TypeInformation.of(CollectionField.class));
        return configSource.getDataStream();
    }

    @Override
    protected void logConfig(DataStream<CollectionField> configSource, ParameterTool params) {
    }

    @Test
    @Ignore
    public void testSingleDestination() {
        fail("Not implemented");
    }

    @Test
    @Ignore
    public void testMultipleDestination() {
        fail("Not implemented");
    }

    @Test
    @Ignore
    public void testFieldFiltering() {
        fail("Not implemented");
    }

    @Test
    @Ignore
    public void testEventFiltering() {
        fail("Not implemented");
    }
}
