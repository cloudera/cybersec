package com.cloudera.cyber;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class TestMessageTypeInformation {

    @Test
    public void testTypeExtraction() {
        TypeInformation<Message> type = TypeInformation.of(Message.class);
        assertThat("Type is not null", type, notNullValue());
    }
}
