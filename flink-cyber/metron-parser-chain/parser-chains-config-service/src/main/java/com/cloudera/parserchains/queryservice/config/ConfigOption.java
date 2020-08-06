package com.cloudera.parserchains.queryservice.config;

public interface ConfigOption<T, V> {

  T get(V source);

}
