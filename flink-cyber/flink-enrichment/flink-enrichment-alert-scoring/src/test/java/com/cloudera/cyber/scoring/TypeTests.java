package com.cloudera.cyber.scoring;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class TypeTests {
    @Test
    public void testScoringRule() {
        TypeInformation<ScoringRule> ti = TypeInformation.of(ScoringRule.class);
        assertThat(ti, notNullValue());
    }

    @Test
    public void testScoringRuleCommand() {
        TypeInformation<ScoringRuleCommand> ti = TypeInformation.of(ScoringRuleCommand.class);
        assertThat(ti, notNullValue());
    }
}
