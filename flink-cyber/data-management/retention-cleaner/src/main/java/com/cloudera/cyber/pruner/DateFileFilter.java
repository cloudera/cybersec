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

package com.cloudera.cyber.pruner;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

@Slf4j
class DateFileFilter extends AbstractDateFilter {

    protected DateFileFilter(FileSystem fileSystem, long maxAge) {
        super(fileSystem, maxAge);
    }

    @Override
    protected boolean accept(FileStatus status) {
        return !status.isDirectory() &&
                status.getModificationTime() < earliestAllowed;
    }
}
