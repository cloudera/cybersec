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

CREATE TABLE IF NOT EXISTS ${profile_metadata_table_name} (
    ID BIGINT PRIMARY KEY,
    PROFILE_GROUP_NAME VARCHAR,
    KEY_FIELD_NAMES VARCHAR,
    PERIOD_DURATION BIGINT,
    PERIOD_DURATION_UNIT VARCHAR,
    STATS_SLIDE BIGINT,
    STATS_SLIDE_UNIT VARCHAR
)