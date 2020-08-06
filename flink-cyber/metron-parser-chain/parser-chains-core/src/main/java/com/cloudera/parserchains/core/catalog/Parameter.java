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
     * Defines the type of widget presented to the user when
     * configuring the parameter.
     * <p>This value is optional and defaults to a simple text box.
     */
    WidgetType widgetType() default WidgetType.TEXT;
}
