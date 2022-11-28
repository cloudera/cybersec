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

package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.Parameter;
import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.model.define.ConfigValueSchema;
import com.cloudera.parserchains.core.model.define.ParserSchema;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cloudera.parserchains.core.utils.AnnotationUtils.getAnnotatedMethodsInOrder;
import static com.cloudera.parserchains.core.utils.AnnotationUtils.getAnnotatedParameters;

/**
 * A {@link ParserBuilder} that uses Java's Reflection API to build and configure a {@link Parser}.
 */
@Slf4j
public class ReflectiveParserBuilder implements ParserBuilder {

    @Override
    public Parser build(ParserInfo parserInfo, ParserSchema parserSchema) throws InvalidParserException {
        Parser parser = construct(parserInfo, parserSchema);
        configure(parser, parserSchema);
        return parser;
    }

    private Parser construct(ParserInfo parserInfo, ParserSchema parserSchema) throws InvalidParserException {
        try {
            Constructor<? extends Parser> constructor = parserInfo.getParserClass().getConstructor();
            return constructor.newInstance();

        } catch (NoSuchMethodException e) {
            throw new InvalidParserException(parserSchema, "A default constructor is missing.", e);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new InvalidParserException(parserSchema, "Unable to instantiate the parser.", e);
        }
    }

    private void configure(Parser parser, ParserSchema parserSchema) throws InvalidParserException {
        // which methods need to be invoked to configure this parser?
        Map<Configurable, Method> annotatedMethods = getAnnotatedMethodsInOrder(parser.getClass());

        final Map<String, List<ConfigValueSchema>> parserConfig = parserSchema.getConfig();

        for (Map.Entry<Configurable, Method> entry : annotatedMethods.entrySet()) {
            final Configurable key = entry.getKey();
            final String annotationKey = key.key();
            final Method method = entry.getValue();

            final List<ConfigValueSchema> valuesSchema = parserConfig.get(annotationKey);

            if (valuesSchema != null) {
                // execute each method with values present in schema
                for (ConfigValueSchema value : valuesSchema) {
                    invokeMethod(parser, parserSchema, annotationKey, method, value.getValues());
                }
            } else if (key.required()) {
                throw new InvalidParserException(parserSchema,
                        String.format("Required field isn't provided: %s", annotationKey));
            }
        }
    }

    private void invokeMethod(Parser parser,
                              ParserSchema parserSchema,
                              String configKey,
                              Method method,
                              Map<String, String> configValues) throws InvalidParserException {
        List<Parameter> annotatedParams = getAnnotatedParameters(method);
        List<String> methodArgs = buildMethodArgs(annotatedParams, configValues);
        log.info(String.format("Invoking method %s(%s); key=%s, parser=%s", method.getName(), methodArgs, configKey, parser.getClass().getName()));

        try {
            method.invoke(parser, methodArgs.toArray());

        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            String message = String.format("Failed to invoke method %s(%s); key=%s, parser=%s",
                    method.getName(), methodArgs, configKey, parser.getClass().getName());
            throw new InvalidParserException(parserSchema, message, e);
        }
    }

    private List<String> buildMethodArgs(List<Parameter> parameterAnnotations,
                                         Map<String, String> configValues) {
        List<String> methodArgs = new ArrayList<>();
        if (parameterAnnotations.size() > 0) {
            // use the parameter annotations, if they exist
            for (Parameter annotation : parameterAnnotations) {
                String value = configValues.get(annotation.key());
                methodArgs.add(value);
            }
        } else {
            // no parameter annotations, use the method annotation
            methodArgs.addAll(configValues.values());
        }
        return methodArgs;
    }
}
