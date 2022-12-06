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

package com.cloudera.parserchains.core.model.config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigDescriptorTest {

    @Test
    void oneAcceptedValue() {
        ConfigKey inputFieldKey = ConfigKey.builder()
                .key("inputField")
                .label("Input Field")
                .description("The input field to use.")
                .build();
        ConfigDescriptor descriptor = ConfigDescriptor
                .builder()
                .name("inputField")
                .description("Input Field")
                .isRequired(true)
                .acceptsValue(inputFieldKey)
                .build();
        assertEquals("inputField", descriptor.getName().get());
        assertEquals("Input Field", descriptor.getDescription().get());
        assertEquals(true, descriptor.isRequired());
        assertEquals(1, descriptor.getAcceptedValues().size());
        assertThat(descriptor.getAcceptedValues(), hasItem(inputFieldKey));
    }

    @Test
    void twoAcceptedValues() {
        ConfigKey fromFieldKey = ConfigKey.builder()
                .key("from")
                .label("From")
                .description("The original name of the field.")
                .build();
        ConfigKey toFieldKey = ConfigKey.builder()
                .key("to")
                .label("To")
                .description("The new name of the field.")
                .build();
        ConfigDescriptor descriptor = ConfigDescriptor
                .builder()
                .name("fieldToRename")
                .description("Field to Rename")
                .isRequired(true)
                .acceptsValue(fromFieldKey)
                .acceptsValue(toFieldKey)
                .build();
        assertEquals("fieldToRename", descriptor.getName().get());
        assertEquals("Field to Rename", descriptor.getDescription().get());
        assertEquals(true, descriptor.isRequired());
        assertEquals(2, descriptor.getAcceptedValues().size());
        assertThat(descriptor.getAcceptedValues(), hasItem(fromFieldKey));
        assertThat(descriptor.getAcceptedValues(), hasItem(toFieldKey));
    }

    @Test
    void requiresName() {
        assertThrows(RuntimeException.class, () -> ConfigDescriptor
                .builder()
                .isRequired(true)
                .build());
    }

    @Test
    void requiresValues() {
        assertThrows(RuntimeException.class, () -> ConfigDescriptor
                .builder()
                .name("fieldToRename")
                .description("Field to Rename")
                .isRequired(true)
                .build());
    }

    @Test
    void nameNotRequired() {
        ConfigKey inputFieldKey = ConfigKey.builder()
                .key("inputField")
                .label("Input Field")
                .description("The input field to use.")
                .build();
        ConfigDescriptor descriptor = ConfigDescriptor
                .builder()
                .description("Input Field")
                .isRequired(true)
                .acceptsValue(inputFieldKey)
                .build();
        assertEquals("inputField", descriptor.getName().get(),
                "Expected the name to have been populated automatically.");
        assertEquals("Input Field", descriptor.getDescription().get());
        assertEquals(true, descriptor.isRequired());
        assertEquals(1, descriptor.getAcceptedValues().size());
        assertThat(descriptor.getAcceptedValues(), hasItem(inputFieldKey));
    }

    @Test
    void descriptionNotRequired() {
        ConfigKey inputFieldKey = ConfigKey.builder()
                .key("inputField")
                .label("Input Field")
                .description("The input field to use.")
                .build();
        ConfigDescriptor descriptor = ConfigDescriptor
                .builder()
                .description("Input Field")
                .isRequired(true)
                .acceptsValue(inputFieldKey)
                .build();
        assertEquals("Input Field", descriptor.getDescription().get(),
                "Expected the description to have been populated automatically.");
        assertEquals("inputField", descriptor.getName().get());
        assertEquals(true, descriptor.isRequired());
        assertEquals(1, descriptor.getAcceptedValues().size());
        assertThat(descriptor.getAcceptedValues(), hasItem(inputFieldKey));
    }
}
