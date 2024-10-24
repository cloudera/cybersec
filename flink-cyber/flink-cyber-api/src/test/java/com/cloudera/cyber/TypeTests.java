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

package com.cloudera.cyber;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.cloudera.cyber.commands.EnrichmentCommand;
import java.util.Map;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.junit.Test;

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

    @Test
    public void testEnrichmentCommand() {
        TypeInformation t = TypeInformation.of(EnrichmentCommand.class);
        assertThat(t, notNullValue());
        assertThat(t, isA(AvroTypeInfo.class));
        assertThat(t.getArity(), equalTo(3));
    }
}
