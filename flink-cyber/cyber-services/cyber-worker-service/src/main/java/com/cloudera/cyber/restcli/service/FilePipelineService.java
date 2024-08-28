package com.cloudera.cyber.restcli.service;

import com.cloudera.cyber.restcli.configuration.AppWorkerConfig;
import com.cloudera.service.common.Utils;
import com.cloudera.service.common.response.Job;
import com.cloudera.service.common.utils.ArchiveUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilePipelineService {
    private final AppWorkerConfig config;

    public void createEmptyPipeline(String pipelineName, String branchName) {
        String fullPath = this.config.getPipelineDir().endsWith("/") ? this.config.getPipelineDir() + pipelineName + "/" + branchName
                : this.config.getPipelineDir() + "/" + pipelineName + "/" + branchName;
        File directory = new File(fullPath);
        if (directory.mkdirs()) {
            log.info("Create full path {}", fullPath);
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cs-create-pipeline", pipelineName);
            processBuilder.directory(directory);
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException ioe) {
            log.error("Caught get IOException {} ", ioe.getMessage());
        } catch (InterruptedException e) {
            log.error("Caught Interrupt Exception with message {} ", e.getMessage());
        }
    }

    public void extractPipeline(byte[] payload, String pipelineName, String branch) throws IOException {
        String fullPipelinePath = pipelineName.endsWith("/") ? this.config.getPipelineDir() + pipelineName + "/" + branch : this.config.getPipelineDir() + "/" + pipelineName + "/" + branch;
        ArchiveUtil.decompressFromTarGzInMemory(payload, fullPipelinePath, true);
    }

    public void startPipelineJob(String pipelineName, String branch, String profileName, String parserName, List<String> jobsNames) throws IOException {
        String fullPipelinePath = pipelineName.endsWith("/") ? this.config.getPipelineDir() + pipelineName + "/" + branch
                : this.config.getPipelineDir() + "/" + pipelineName + "/" + branch;
        jobsNames.stream().map(jobName -> {
            Job.JobType jobType = Utils.getEnumFromString(jobName, Job.JobType.class, Job.JobType::getName);
            String confName = getConfName(profileName, parserName, jobType);
            return Job.builder()
                    .jobPipeline(pipelineName)
                    .jobType(jobType)
                    .jobBranch(branch)
                    .confName(confName)
                    .build();
        }).forEach(job -> {
            try {
                job.getJobType().getScript(job);
                ProcessBuilder processBuilder = new ProcessBuilder(job.getJobType().getScript(job));
                processBuilder.directory(new File(fullPipelinePath));
                Process process = processBuilder.start();
                Thread clt = new Thread(() -> {
                    try {
                        log.info(IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
                        log.error(IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        log.error("Error happens on stream reading from bash {}", e.getMessage());
                    }
                });
                clt.setDaemon(true);
                clt.start();
            } catch (IOException e) {
                log.error("Couldn't start job with name '{}', cause '{}'", job.getJobFullName(), e.getMessage());
            }
        });
    }

    private static String getConfName(String profileName, String parserName, Job.JobType jobType) {
        switch (jobType) {
            case PARSER:
                return StringUtils.defaultString(parserName, "main");
            case PROFILE:
                return StringUtils.defaultString(profileName, "main");
            case TRIAGE:
            case GENERATOR:
            case INDEX:
            default:
                return null;
        }
    }
}