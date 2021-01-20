package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;

import java.security.*;
import java.util.Base64;

public abstract class AbstractParserJobTest extends ParserJob {
    protected ManualSource<MessageToParse> source;
    protected CollectingSink<Message> sink = new CollectingSink<>();
    protected CollectingSink<Message> errorSink = new CollectingSink<>();

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    protected void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results) {
    }

    @Override
    protected void writeErrors(ParameterTool params, DataStream<Message> errors) {
        errors.addSink(errorSink);
    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(MessageToParse.class));
        return source.getDataStream();
    }

    public abstract void testParser() throws Exception;

    protected String getKeyBase64() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        gen.initialize(1024, new SecureRandom());
        KeyPair pair = gen.generateKeyPair();
        return Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
    }

}
