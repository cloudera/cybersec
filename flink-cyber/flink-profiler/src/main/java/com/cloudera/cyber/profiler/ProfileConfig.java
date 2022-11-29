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

package com.cloudera.cyber.profiler;


import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@Builder
public class ProfileConfig {
    private String name;
    private String source;
    private String entity;
    private String filter;
    private Map<String, String> fields;
    private Integer interval;
    private TimeUnit intervalUnit;
    private ProfileMode mode;
    private Long latenessTolerance;
}