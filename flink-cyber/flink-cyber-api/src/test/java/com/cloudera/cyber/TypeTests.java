package com.cloudera.cyber;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class TypeTests {

    @Test
    public void testTypes() {
        TypeInformation<EnrichmentEntry> types = TypeInformation.of(EnrichmentEntry.class);

        TypeInformation<Map<String, String>> mapType = TypeInformation.of(new TypeHint<Map<String, String>>() {
        });

        System.out.println(types);
    }

    @Test
    public void testDataQuality() {
        TypeInformation t = TypeInformation.of(DataQualityMessage.class);
        assertThat(t, notNullValue());
    }

    @Test
    public void testMessageTypes() {
        TypeInformation t = TypeInformation.of(Message.class);
        assertThat(t, notNullValue());
        assertThat(t, isA(AvroTypeInfo.class));
        assertThat(t.getArity(), equalTo(8));


        //assertThat(t().get("dataQualityMessages"), isA(PojoField.class));
    }
}
