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

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class DateFileFilterTest {

    @Test
    public void testIgnoresDirectories() throws Exception {
        FileSystem testFS = mock(FileSystem.class);
        Path file = new Path("/tmp");
        when(testFS.getFileStatus(any())).thenReturn(makePath(file));
        DateFileFilter filter = new DateFileFilter(testFS, 30 * 3600000);
        assertFalse(filter.accept(file), "Should ignore directories");
    }


    @Test
    void testIgnoresNewerFiles() throws IOException {
        FileSystem testFS = mock(FileSystem.class);
        Path file = new Path("/tmp.txt");
        when(testFS.getFileStatus(file)).thenReturn(makeFile(file));
        assertFalse(new DateFileFilter(testFS, 1000).accept(file), "Should ignore new files");
    }

    @Test
    void testProcessesOlderFiles() throws IOException {
        FileSystem testFS = mock(FileSystem.class);
        Path file = new Path("/tmp.txt");
        when(testFS.getFileStatus(file)).thenReturn(makeFile(file, new Date().getTime() - 10000));
        assertTrue(new DateFileFilter(testFS, 1000).accept(file), "Should accept old files");
    }

    private FileStatus makeFile(Path path) {
        return makeFile(path, new Date().getTime());
    }

    private FileStatus makeFile(Path path, long time) {
        return new FileStatus(0, false, 0, 0, time, path);
    }

    private FileStatus makePath(Path path) {
        return makePath(path, new Date().getTime());
    }

    private FileStatus makePath(Path path, long time) {
        return new FileStatus(0, true, 0, 0, time, path);
    }
}
