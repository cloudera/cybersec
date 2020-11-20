package com.cloudera.cyber.test.generator;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetIpMap implements MapFunction<Tuple2<String, String>, String> {

    private final static Pattern ipExtractPattern = Pattern.compile("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})");

    @Override
    public String map(Tuple2<String, String> s) {
        Matcher matcher = ipExtractPattern.matcher(s.f1);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
}
