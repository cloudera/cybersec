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
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import java.io.IOException;
import java.util.Date;

@Slf4j
abstract class AbstractDateFilter extends Configured implements PathFilter {
    protected final FileSystem fileSystem;
    protected final long maxAge;
    protected final long earliestAllowed;

    protected AbstractDateFilter(FileSystem fileSystem, long maxAge) {
        this.fileSystem = fileSystem;
        this.maxAge = maxAge;
        this.earliestAllowed = new Date().getTime() - this.maxAge;
    }

    @Override
    public final boolean accept(Path path) {
        log.debug("ACCEPT - working with file: {}", path);
        try {
            FileStatus status = fileSystem.getFileStatus(path);
            return accept(status);
        } catch (IOException e) {
            log.error("IOException", e);
            return false;
        }
    }

    protected abstract boolean accept(FileStatus status);
}
