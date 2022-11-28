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

<#assign key_count = field_key_count?number >
CREATE TABLE IF NOT EXISTS ${measurement_data_table_name} (
    MEASUREMENT_ID INTEGER NOT NULL,
    <#list 1..key_count as i>
    KEY_${i} VARCHAR,
    </#list>
    MEASUREMENT_NAME VARCHAR NOT NULL,
    MEASUREMENT_TYPE VARCHAR,
    MEASUREMENT_TIME TIMESTAMP NOT NULL,
    MEASUREMENT_VALUE DOUBLE NOT NULL,
     CONSTRAINT pk PRIMARY KEY(MEASUREMENT_ID, <#list 1..key_count as i>KEY_${i}, </#list>MEASUREMENT_NAME, MEASUREMENT_TYPE, MEASUREMENT_TIME,MEASUREMENT_VALUE)
)