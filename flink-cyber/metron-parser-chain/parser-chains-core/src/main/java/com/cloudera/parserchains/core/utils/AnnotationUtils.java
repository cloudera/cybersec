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

package com.cloudera.parserchains.core.utils;

import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.Parameter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnnotationUtils {

    public static Map<Configurable, Method> getAnnotatedMethods(Class<? extends Parser> clazz) {
        Map<Configurable, Method> results = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (annotation instanceof Configurable) {
                    results.put((Configurable) annotation, method);
                }
            }
        }
        return results;
    }

    public static Map<Configurable, Method> getAnnotatedMethodsInOrder(Class<? extends Parser> clazz) {
        return getAnnotatedMethods(clazz).entrySet().stream()
                                         .sorted(Comparator.comparing(entry -> entry.getKey().orderPriority()))
                                         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                               (first, second) -> first,
                                               LinkedHashMap::new));
    }

    public static List<Parameter> getAnnotatedParameters(Method method) {
        List<Parameter> results = new ArrayList<>();
        for (Annotation[] parameterAnnotationArray : method.getParameterAnnotations()) {
            for (Annotation parameterAnnotation : parameterAnnotationArray) {
                if (parameterAnnotation instanceof Parameter) {
                    results.add((Parameter) parameterAnnotation);
                }
            }
        }
        return results;
    }
}
