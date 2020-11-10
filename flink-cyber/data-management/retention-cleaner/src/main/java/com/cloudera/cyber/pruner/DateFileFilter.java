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
