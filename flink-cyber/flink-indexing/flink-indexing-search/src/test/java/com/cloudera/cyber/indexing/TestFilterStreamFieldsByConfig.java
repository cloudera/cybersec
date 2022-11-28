/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedBroadcastOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.cloudera.cyber.indexing.SearchIndexJob.Descriptors.broadcastState;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class TestFilterStreamFieldsByConfig {

    private FilterStreamFieldsByConfig f;
    private KeyedBroadcastOperatorTestHarness<String, Message, CollectionField, IndexEntry> testHarness;

    @Before
    public void setupTestHarness() throws Exception {

        //instantiate user-defined function
        f = new FilterStreamFieldsByConfig(broadcastState);

        testHarness = ProcessFunctionTestHarnesses
                .forKeyedBroadcastProcessFunction(f, Message::getSource, TypeInformation.of(String.class), broadcastState);

        // optionally configured the execution environment
        testHarness.getExecutionConfig().setAutoWatermarkInterval(50);

        // open the test harness (will also call open() on RichFunctions)
        testHarness.open();
    }

    @Test
    public void basicTest() throws Exception {
        // send some config
        testHarness.processBroadcastElement(CollectionField.builder()
                .key("test")
                .values(Arrays.asList("a", "b"))
                .build(), 0);
        // send a message
        testHarness.processElement(TestUtils.createMessage(), 10);

        List<StreamRecord<? extends IndexEntry>> streamRecords = testHarness.extractOutputStreamRecords();
        assertThat(streamRecords, IsCollectionWithSize.hasSize(1));
        assertThat(streamRecords.get(0).getValue(), isA(IndexEntry.class));
        assertThat(streamRecords.get(0).getValue().getFields(), notNullValue());
    }
}
