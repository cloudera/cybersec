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

package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.config.AppProperties;
import com.cloudera.service.common.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.Path;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final AppProperties appProperties;

    @Cacheable("pipelinePathMap")
    public Map<String, Path> findAll() throws IOException {
        String pipelinePathStr = appProperties.getPipelinesPath();

        final List<FileStatus> statusList = FileUtil.listFiles(pipelinePathStr, false);;
        if (statusList == null) {
            return null;
        }

        final Map<String, Path> pipelineMap = new HashMap<>();
        for (FileStatus fileStatus : statusList) {
            if (fileStatus.isDir()) {
                //check if pipeline is valid
                final Path pipelinePath = fileStatus.getPath();
                final Path chainsPath = new Path(pipelinePath, "parse/chains");

                if (pipelinePath.getFileSystem().exists(chainsPath)) {
                    pipelineMap.put(pipelinePath.getName(), chainsPath);
                }
            }
        }
        return pipelineMap;
    }


    public Set<String> createPipeline(String pipelineName) {
        return null;
    }
}
