package com.cloudera.parserchains.queryservice.common.utils;

import java.util.UUID;

public class UUIDGenerator implements IDGenerator<String> {

    @Override
    public String incrementAndGet() {
        return UUID.randomUUID().toString();
    }
}
