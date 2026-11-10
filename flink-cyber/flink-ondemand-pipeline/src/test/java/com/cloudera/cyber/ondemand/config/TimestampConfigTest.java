package com.cloudera.cyber.ondemand.config;

import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.Map;

public class TimestampConfigTest {

    private static final String TEST_PARAM_KEY = "start.time";

    @Test
    public void testValidTimestamp() {
        LocalDateTime actual = testTimestampConfig("2026-09-01 17:15:00.035");
        Assertions.assertEquals(LocalDateTime.of(2026, Month.SEPTEMBER, 1, 17, 15, 0, 35000000), actual);
    }

    @Test
    public void testInvalidTimestamp() {
        String invalidTimestampString = "XXX YYY";
        RuntimeException actualException = Assertions.assertThrowsExactly(RuntimeException.class, () -> testTimestampConfig(invalidTimestampString));
        String actualMessage = actualException.getMessage();
        Assertions.assertTrue(actualMessage.contains(TEST_PARAM_KEY));
        Assertions.assertTrue(actualMessage.contains(invalidTimestampString));
        Assertions.assertTrue(actualMessage.contains("does not match the expected date format"));
    }

    @Test
    public void testMissingConfigParam() {
        ParameterTool params = ParameterTool.fromMap(Collections.emptyMap());
        RuntimeException actualException = Assertions.assertThrowsExactly(RuntimeException.class, () -> TimestampConfig.getConfiguration(params, TEST_PARAM_KEY));
        String actualMessage = actualException.getMessage();
        Assertions.assertTrue(actualMessage.contains(TEST_PARAM_KEY));
        Assertions.assertTrue(actualMessage.contains("required key"));
    }

    private LocalDateTime testTimestampConfig(String timestampString) {
        Map<String, String> params = Map.of(TEST_PARAM_KEY, timestampString);

        return TimestampConfig.getConfiguration(ParameterTool.fromMap(params), TEST_PARAM_KEY);
    }
}
