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

package org.apache.metron.stellar.common.utils;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.FileReplicator;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponent;
import org.apache.commons.vfs2.provider.VfsComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class UniqueFileReplicator implements VfsComponent, FileReplicator {

    private static final char[] TMP_RESERVED_CHARS = new char[] {'?', '/', '\\', ' ', '&', '"', '\'', '*', '#', ';', ':', '<', '>', '|'};
    private static final Logger log = LoggerFactory.getLogger(UniqueFileReplicator.class);

    private File tempDir;
    private VfsComponentContext context;
    private List<File> tmpFiles = Collections.synchronizedList(new ArrayList<File>());

    public UniqueFileReplicator(File tempDir) {
        this.tempDir = tempDir;
        if (!tempDir.exists() && !tempDir.mkdirs())
            log.warn("Unexpected error creating directory " + tempDir);
    }

    @Override
    public File replicateFile(FileObject srcFile, FileSelector selector) throws FileSystemException {
        String baseName = srcFile.getName().getBaseName();

        try {
            String safeBasename = UriParser.encode(baseName, TMP_RESERVED_CHARS).replace('%', '_');
            File file = File.createTempFile("vfsr_", "_" + safeBasename, tempDir);
            file.deleteOnExit();

            final FileObject destFile = context.toFileObject(file);
            destFile.copyFrom(srcFile, selector);

            return file;
        } catch (IOException e) {
            throw new FileSystemException(e);
        }
    }

    @Override
    public void setLogger(Log logger) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setContext(VfsComponentContext context) {
        this.context = context;
    }

    @Override
    public void init() throws FileSystemException {

    }

    @Override
    public void close() {
        synchronized (tmpFiles) {
            for (File tmpFile : tmpFiles) {
                if (!tmpFile.delete())
                    log.warn("File does not exist: " + tmpFile);
            }
        }

        if (tempDir.exists()) {
            int numChildren = tempDir.list().length;
            if (0 == numChildren && !tempDir.delete())
                log.warn("Cannot delete empty directory: " + tempDir);
        }
    }
}