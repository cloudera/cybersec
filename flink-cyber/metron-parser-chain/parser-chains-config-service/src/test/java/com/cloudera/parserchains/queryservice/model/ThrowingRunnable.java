package com.cloudera.parserchains.queryservice.model;

@FunctionalInterface
public interface ThrowingRunnable {
  void run() throws Exception;
}
