package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.TestUtils;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MessageSourceFilterTest {

    @Test
    public void matchSingleSource() {
        String singleSource = "single_source";
        List<String>  filterSources = Collections.singletonList("single_source");
        testFilterMessage(filterSources, singleSource, true);
        testFilterMessage(filterSources, "non matching source", false);
    }

    @Test
    public void matchMultipleSources() {
        List<String> sources = Lists.newArrayList("source1", "source2", "source3");
        sources.forEach(source -> testFilterMessage(sources, source, true));
        testFilterMessage(sources, "non matching source", false);
    }

    private static void testFilterMessage(List<String> filterSources, String messageSource, boolean matches) {
        Message message = Message.builder()
                .extensions(new HashMap<>())
                .ts(MessageUtils.getCurrentTimestamp())
                .originalSource(TestUtils.source(messageSource, 0, 0))
                .source(messageSource)
                .build();

        MessageSourceFilter filter = new MessageSourceFilter(filterSources);
        Assert.assertEquals(matches, filter.filter(message));
    }

}
