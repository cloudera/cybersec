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
