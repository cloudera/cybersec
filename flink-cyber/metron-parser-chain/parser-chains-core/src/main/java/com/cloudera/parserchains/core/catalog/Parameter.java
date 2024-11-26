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

package com.cloudera.parserchains.core.catalog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows a parser author to describe how their parser can be configured.
 *
 * <p>A parser author can use this annotation along with {@link Configurable}
 * to describe how their parser can be configured.
 *
 * <p>This should be used to annotate a method parameter that can be used
 * to configure the parser.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {

    /**
     * A unique key for this configurable parameter.
     * <p>This value is required.
     */
    String key();

    /**
     * A label for this configurable parameter that is displayed to the user.
     * <p>This value is optional.
     */
    String label() default "";

    /**
     * A description of this configurable parameter that is displayed to
     * the user.
     * <p>This value is optional.
     */
    String description() default "";

    /**
     * Whether the user is required to provide a value for this configurable parameter.
     * <p>If true, the user must provide a value. Otherwise, false.
     * <p>This value is optional.
     */
    boolean required() default false;

    /**
     * The default value of this configurable parameter.
     * <p>This value is optional.
     */
    String defaultValue() default "";

    /**
     * Specifies if the parameter specifies the output field name.
     * If true, the selection will be provided with possible field names.
     * <p>This value is optional.
     */
    boolean isOutputName() default false;

    /**
     * If true, the value will be treated as path and can be appended with base directory.
     * <p>This value is optional.
     */
    boolean isPath() default false;

    /**
     * Defines the type of widget presented to the user when
     * configuring the parameter.
     * <p>This value is optional and defaults to a simple text box.
     */
    WidgetType widgetType() default WidgetType.TEXT;
}
