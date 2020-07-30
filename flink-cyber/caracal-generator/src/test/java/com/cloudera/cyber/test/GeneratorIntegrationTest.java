package com.cloudera.cyber.test;

import com.cloudera.cyber.test.generator.CaracalGeneratorFlinkJob;
import com.salesforce.kafka.test.KafkaTestUtils;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.runtime.testutils.MiniClusterResourceConfiguration;
import org.apache.flink.test.util.MiniClusterWithClientResource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.test.generator.CaracalGeneratorFlinkJob.PARAMS_RECORDS_LIMIT;

public class GeneratorIntegrationTest {

    @ClassRule
    public static MiniClusterWithClientResource flinkCluster =
            new MiniClusterWithClientResource(
                    new MiniClusterResourceConfiguration.Builder()
                            .setNumberSlotsPerTaskManager(2)
                            .setNumberTaskManagers(1)
                            .build());


    @ClassRule
    public static final SharedKafkaTestResource
            sharedKafkaTestResource = new SharedKafkaTestResource();


    @Test(timeout = 50000)
    @Ignore("Flakey integration test")
    public void testGenerator() throws Exception {
        getKafkaTestUtils().createTopic("generator.metrics" ,1, (short) 1);

        String bootstrap = sharedKafkaTestResource.getKafkaBrokers()
                .stream().map(b -> b.getConnectString()).collect(Collectors.joining(","));


        Map<String, String> params = new HashMap<>();
        params.put("kafka.bootstrap.servers", bootstrap);
        params.put(PARAMS_RECORDS_LIMIT, "500");
        new CaracalGeneratorFlinkJob().run(ParameterTool.fromMap(params));

        List<ConsumerRecord<byte[], byte[]>> output = getKafkaTestUtils().consumeAllRecordsFromTopic("generator.metrics");
    }

    private KafkaTestUtils getKafkaTestUtils() {
        return sharedKafkaTestResource.getKafkaTestUtils();
    }

}
