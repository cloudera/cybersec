package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.TestUtils;
import lombok.extern.log4j.Log4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static com.cloudera.cyber.flink.Utils.getResourceAsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;

@Log4j
@Ignore
public class TestStixJobWithMessages extends TestStixJob {

    @Test
    @Ignore("TODO - fix concurrency issue")
    public void testWithMessage() throws Exception {

        StreamExecutionEnvironment env = createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
            put("threatIntelligence.ip", "Address:ipv4_addr");
        }}));
        env.setParallelism(1);

        JobTester.startTest(env);

        source.sendRecord(getResourceAsString("domain.xml"),0L);
        source.sendRecord(getResourceAsString("domain2.xml"),0L);
        source.sendRecord(getResourceAsString("ip.xml"),0L);
        source.sendRecord(getResourceAsString("sample.xml"),0L);

        source.sendWatermark(1000L);

        messageSource.sendRecord(Message.builder()
                .ts(0L)
                .originalSource(TestUtils.createOriginal())
                .extensions(Collections.singletonMap("ip", "192.168.0.1"))
                .build(), 1500L);

        JobTester.stopTest();

        Message out = resultsSink.poll();
        assertThat("Threats have been found", out.getThreats(), allOf(
                IsMapContaining.hasKey("ip"),
                IsMapContaining.hasEntry("ip", IsCollectionWithSize.hasSize(1))));

    }
}
