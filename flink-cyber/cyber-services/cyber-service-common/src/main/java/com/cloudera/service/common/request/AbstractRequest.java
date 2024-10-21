package com.cloudera.service.common.request;

import lombok.Data;

@Data
public abstract class AbstractRequest {
    protected final String requestId;
}
