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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

/**
 * Prune data from the cloudera cyber reference implementation
 * <p>
 * Heavily inspired by https://github.com/apache/metron/blob/master/metron-platform/metron-data-management/src/main/java/org/apache/metron/dataloads/bulk/HDFSDataPruner.java
 */
@Slf4j
@RequiredArgsConstructor
public class Pruner {

    @NonNull
    private String fileSystemUri;
    @NonNull
    private String hiveUri;
    @NonNull
    private String hiveUser;
    @NonNull
    private String hivePassword;
    @NonNull
    private PrunerConfig config;

    transient FileSystem _fileSystem;

    public FileSystem getFileSystem() throws IOException {
        if (_fileSystem == null) {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", fileSystemUri);
            this._fileSystem = FileSystem.get(conf);
        }
        return this._fileSystem;
    }

    long prune() throws IOException, SQLException {
        long filesPruned = 0L;

        // prune originals
        filesPruned += pruneFiles(config.getOriginalLocation() + "/**/**", config.getOriginalsMaxMs());
        pruneDirs(config.getOriginalLocation() + "/**", config.getOriginalsMaxMs());

        // prune logs
        filesPruned += pruneFiles(config.getLogsLocation() + "/**/**", config.getLogsMaxMs());
        pruneDirs(config.getLogsLocation() + "/**", config.getLogsMaxMs());

        // prune hive tables
        if (!hiveUri.isEmpty()) deleteFromHive(hiveUri, hiveUser, hivePassword, config.getEventsTable());

        return filesPruned;
    }

    private int deleteFromHive(String hiveUri, String hiveUser, String hivePassword, String eventsTable) throws SQLException {
        int rows;
        String sql = String.format("DELETE FROM %s WHERE ts > %d", eventsTable, new Date().getTime() - config.getEventsMaxMs());
        try (Connection connection = DriverManager.getConnection(hiveUri, hiveUser, hivePassword)) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            rows = preparedStatement.executeUpdate();
        }
        return rows;
    }

    private void pruneDirs(String globPath, long maxAge) throws IOException {
        // object store filesystems don't need to remove paths
        if (globPath.startsWith("s3") || globPath.startsWith("adls:")) return;

        // remove directories within the date range which are empty
        FileStatus[] filesToDelete = getFileSystem().globStatus(new Path(globPath), new DateDirectoryFilter(getFileSystem(), maxAge, config.getDateFormatter()));
        for (FileStatus fileStatus : filesToDelete) {
            log.debug("Deleting File: {}", fileStatus.getPath());
            getFileSystem().delete(fileStatus.getPath(), false);
        }
    }

    private long pruneFiles(String globPath, long maxAge) throws IOException {
        long filesPruned = 0;
        FileStatus[] filesToDelete = getFileSystem().globStatus(new Path(globPath), new DateFileFilter(getFileSystem(), maxAge));
        for (FileStatus fileStatus : filesToDelete) {
            log.info("Deleting File: {}", fileStatus.getPath());
            getFileSystem().delete(fileStatus.getPath(), false);
            filesPruned++;
        }
        return filesPruned;
    }

    public static void main(String[] args) {
        Options options = new Options();
        Options help = new Options();

        {
            Option o = new Option("h", "help", false, "This screen");
            o.setRequired(false);
            help.addOption(o);
        }
        {
            Option o = new Option("f", "filesystem", true, "Filesystem uri - e.g. hdfs://host:8020 or file:///");
            o.setArgName("FILESYSTEM");
            o.setRequired(false);
            options.addOption(o);
        }
        {
            Option o = new Option("i", "hive", true, "Hive uri - e.g. jdbc:hive2://");
            o.setArgName("HIVE");
            o.setRequired(false);
            options.addOption(o);
        }
        {
            Option o = new Option("u", "user", true, "Hive user");
            o.setArgName("HIVEUSER");
            o.setRequired(false);
            options.addOption(o);
        }
        {
            Option o = new Option("p", "password", true, "Hive password");
            o.setArgName("HIVEPASSWORD");
            o.setRequired(false);
            options.addOption(o);
        }
        {
            Option o = new Option("c", "config-file", true, "Config File to laod retention details from");
            o.setArgName("CONFIG");
            o.setRequired(true);
            options.addOption(o);
        }

        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;

            try {
                cmd = parser.parse(help, args, true);
                if (cmd.getOptions().length > 0) {
                    final HelpFormatter usageFormatter = new HelpFormatter();
                    usageFormatter.printHelp(Pruner.class.getName(), null, options, null, true);
                    System.exit(0);
                }
                cmd = parser.parse(options, args);
            } catch (ParseException pe) {
                final HelpFormatter usageFormatter = new HelpFormatter();
                usageFormatter.printHelp(Pruner.class.getName(), null, options, null, true);
                System.exit(-1);
            }

            PrunerConfig config = new ObjectMapper().readValue(new File(cmd.getOptionValue("c")), PrunerConfig.class);

            Pruner pruner = new Pruner(
                    cmd.getOptionValue("f", new Configuration().get("fs.defaultFS")),
                    cmd.getOptionValue("i", ""),
                    cmd.getOptionValue("u", ""),
                    cmd.getOptionValue("p", ""),
                    config);
            Long pruned = pruner.prune();

            log.info("Pruned {} files using {}", pruned, config);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


}
