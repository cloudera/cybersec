package com.cloudera.cyber.libs;

import com.cloudera.cyber.CyberFunction;
import org.atteo.classindex.ClassIndex;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CyberFunctionUtils {

    public static Stream<Class<?>> findAll() {
        return StreamSupport.stream(ClassIndex.getAnnotated(CyberFunction.class).spliterator(), true);
    };
}
