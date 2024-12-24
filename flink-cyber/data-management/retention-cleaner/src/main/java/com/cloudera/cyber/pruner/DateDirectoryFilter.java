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

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

@Slf4j
public class DateDirectoryFilter extends AbstractDateFilter {

    private final DateFormat df;

    protected DateDirectoryFilter(FileSystem fileSystem, long maxAge, DateFormat df) {
        super(fileSystem, maxAge);
        this.df = df;
    }

    @Override
    protected boolean accept(FileStatus status) {
        return status.isDirectory()
               && getDirectoryDate(status.getPath()) < earliestAllowed
               && directoryEmpty(status);
    }

    private boolean directoryEmpty(FileStatus status) {
        try {
            return fileSystem.listStatus(status.getPath()).length == 0;
        } catch (IOException e) {
            log.warn(String.format("Directory empty test failed on %s", status.getPath(), e));
            return true;
        }
    }

    private long getDirectoryDate(Path path) {
        try {
            return df.parse(path.getName()).getTime();
        } catch (ParseException e) {
            log.error("Cannot parse path as date", e);
            return Long.MAX_VALUE;
        }
    }
}
