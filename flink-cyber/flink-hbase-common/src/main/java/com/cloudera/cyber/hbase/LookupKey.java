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

package com.cloudera.cyber.hbase;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;

import java.util.Map;

@Getter
@SuperBuilder
@EqualsAndHashCode
public abstract class LookupKey {
    private String cf;
    private String tableName;
    private String key;

    public abstract Get toGet();
    public abstract Map<String, Object> resultToMap(Result hbaseResult);
}