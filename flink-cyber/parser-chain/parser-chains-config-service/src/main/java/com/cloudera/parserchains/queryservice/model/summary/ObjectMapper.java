package com.cloudera.parserchains.queryservice.model.summary;

public interface ObjectMapper <T, R> {

    T reform(R source);

    R transform(T source);
}
