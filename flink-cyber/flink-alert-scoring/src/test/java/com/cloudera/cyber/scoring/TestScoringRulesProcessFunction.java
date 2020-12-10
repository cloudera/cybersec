package com.cloudera.cyber.scoring;

import org.apache.flink.streaming.util.BroadcastOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class TestScoringRulesProcessFunction {


    @Test
    public void testProcessFunction() throws Exception {
        BroadcastOperatorTestHarness<Integer, String, Integer> harness = ProcessFunctionTestHarnesses
                .forBroadcastProcessFunction(new ScoringProcessFunction());

        harness.processBroadcastElement(ScoringRuleCommand.builder().rule());
        harness.processElement(1, 10);

        assertEquals(harness.extractOutputValues(), Arrays.asList(0, 1));

    }
}
