package com.cloudera.parserchains.core.utils;

import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.Parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        Map<Configurable, Method> results = new TreeMap<>(Comparator.comparing(Configurable::orderPriority));
        for (Method method : clazz.getDeclaredMethods()) {
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (annotation instanceof Configurable) {
                    results.put((Configurable) annotation, method);
                }
            }
        }
        return results;
    }

    public static List<Parameter> getAnnotatedParameters(Method method) {
        List<Parameter> results = new ArrayList<>();
        for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
            for (Annotation aParameterAnnotation : parameterAnnotations) {
                if (aParameterAnnotation instanceof Parameter) {
                    results.add((Parameter) aParameterAnnotation);
                }
            }
        }
        return results;
    }
}
