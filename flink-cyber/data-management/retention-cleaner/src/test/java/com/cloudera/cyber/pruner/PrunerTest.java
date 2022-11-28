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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrunerTest {
    private Date todaysDate;
    private Date yesterday = new Date();


    @BeforeAll
    public static void beforeClass() {
    }

    @BeforeEach
    public void setUp() {
        Calendar today = Calendar.getInstance();
        today.clear(Calendar.HOUR);
        today.clear(Calendar.MINUTE);
        today.clear(Calendar.SECOND);
        todaysDate = today.getTime();
        yesterday.setTime(todaysDate.getTime() - TimeUnit.DAYS.toMillis(1));
    }

    @Test
    public void testDeletesCorrectFiles() throws Exception {
        Path temp = Files.createTempDirectory("temp");
        File dataPath = temp.toFile();
        dataPath.deleteOnExit();

        createTestFiles(dataPath);

        Pruner pruner = new Pruner("file://" + dataPath.getAbsolutePath(), "", "", "", PrunerConfig.builder()
                .originalLocation(dataPath.getAbsolutePath() + "/data/originals/")
                .logsLocation(dataPath.getAbsolutePath() + "/data/logs/")
                .originalsMaxAge(8)
                .logsMaxAge(8)
                .build());

        long prunedCount = pruner.prune();
        assertEquals(45, prunedCount, "Should have pruned 45 files- pruned: " + prunedCount);

        File[] filesLeft = new File(dataPath.getAbsolutePath() + "/data/originals/2020-01-02--03").listFiles();
        File[] filesList = new File[filesLeft.length];
        for (int i = 0; i < 5; i++) {
            filesList[i] = new File(dataPath.getAbsolutePath() + "/data/originals/2020-01-02--03/file-" + String.format("%02d", i));
        }

        Arrays.sort(filesLeft);
        assertArrayEquals(filesLeft, filesList, "First four files should have been left behind");
    }

    private void createTestFiles(File dataPath) throws IOException {
        dataPath.mkdirs();
        new File(dataPath.getAbsolutePath() + "/data/originals/2020-01-02--03").mkdirs();
        new File(dataPath.getAbsolutePath() + "/data/logs/2020-01-02--03").mkdirs();

        //create files
        for (int i = 0; i < 50; i++) {
            File file = new File(dataPath.getAbsolutePath() + "/data/originals/2020-01-02--03/file-" + String.format("%02d", i));
            file.createNewFile();
            file.deleteOnExit();
        }

        //Set modification date today - 1 day
        for (int i = 5; i < 25; i++) {
            File file = new File(dataPath.getAbsolutePath() + "/data/originals/2020-01-02--03/file-" + String.format("%02d", i));
            file.setLastModified(todaysDate.getTime() - TimeUnit.DAYS.toMillis(1));
            file.deleteOnExit();
        }

        //Set modification date today - 10 days
        for (int i = 25; i < 40; i++) {
            File file = new File(dataPath.getAbsolutePath() + "/data/originals/2020-01-02--03/file-" + String.format("%02d", i));
            file.setLastModified(todaysDate.getTime() - TimeUnit.DAYS.toMillis(10));
            file.deleteOnExit();
        }

        //Set modification date today - 20 days
        for (int i = 40; i < 50; i++) {
            File file = new File(dataPath.getAbsolutePath() + "/data/originals/2020-01-02--03/file-" + String.format("%02d", i));
            file.setLastModified(todaysDate.getTime() - TimeUnit.DAYS.toMillis(20));
            file.deleteOnExit();
        }
    }
}
