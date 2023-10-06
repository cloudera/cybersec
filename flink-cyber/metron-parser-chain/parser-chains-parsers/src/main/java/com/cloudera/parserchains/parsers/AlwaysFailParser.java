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

package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.RouterLink;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link Parser} that always fails.
 *
 * <p>This can be used with a {@link RouterLink}
 * to flag when unexpected conditions are encountered in the data.
 */
@MessageParser(
    name="Error",
    description = "Always results in an error. Can be used with a router to flag unexpected data.")
public class AlwaysFailParser implements Parser {
    private static final String DEFAULT_ERROR_MESSAGE = "Parsing error encountered";
    private Throwable error;

    public AlwaysFailParser() {
        error = new IllegalStateException(DEFAULT_ERROR_MESSAGE);
    }

    @Override
    public Message parse(Message message) {
        return Message.builder()
                .withFields(message)
                .withError(error)
                .build();
    }

    @Configurable(key="errorMessage")
    public AlwaysFailParser withError(
            @Parameter(key="errorMessage",
                    label="Error Message",
                    description="The error message explaining the error. Default value: '" + DEFAULT_ERROR_MESSAGE + "'",
                    defaultValue=DEFAULT_ERROR_MESSAGE) String message) {
        if(StringUtils.isNotEmpty(message)) {
            error = new IllegalStateException(message);
        }
        return this;
    }

    public AlwaysFailParser withError(Throwable error) {
        this.error = Objects.requireNonNull(error, "A valid error message is required.");
        return this;
    }

    Throwable getError() {
        return error;
    }
}
