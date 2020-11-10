package com.cloudera.cyber.pruner;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;

@Slf4j
public class DateDirectoryFilter extends AbstractDateFilter {

    private final DateFormat df;

    protected DateDirectoryFilter(FileSystem fileSystem, long maxAge, DateFormat df) {
        super(fileSystem, maxAge);
        this.df = df;
    }

    @Override
    protected boolean accept(FileStatus status) {
        return status.isDirectory() &&
                getDirectoryDate(status.getPath()) < earliestAllowed &&
                directoryEmpty(status);
    }

    private boolean directoryEmpty(FileStatus status) {
        try {
            return fileSystem.listStatus(status.getPath()).length == 0;
        } catch(IOException e) {
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
