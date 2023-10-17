package com.cloudera.cyber.restcli.service;

import com.cloudera.service.common.Utils;
import com.cloudera.service.common.response.Job;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JobService {
    @Value("${cluster.pipeline.dir}")
    private String pipelineDir;
    public static final String LOG_CLI_JOB_INFO = "Successfully read jobs from cli with exit code {}. job count '{}' jobs data '[{}]'";
    private final Pattern pattern = Pattern.compile("^(?<date>[\\d.:\\s]+)\\s:\\s(?<jobId>[a-fA-F0-9]+)\\s:\\s(?<jobFullName>[\\w.-]+)\\s\\((?<jobStatus>\\w+)\\)$");



    public List<Job> getJobs() throws IOException, InterruptedException {
        List<Job> jobs = new ArrayList<>();
        int exitValue = fillJobList(jobs);
        log.info(LOG_CLI_JOB_INFO, exitValue, jobs.size(), jobs.stream().map(Objects::toString).collect(Collectors.joining(",")));
        return jobs;    }

    public Job getJob(String id) throws IOException, InterruptedException {
        List<Job> jobs = getJobs();
        return jobs.stream()
                .filter(job -> StringUtils.equalsIgnoreCase(job.getJobId().toHexString(), id))
                .findFirst()
                .orElse(null);
    }

    public Job restartJob(String id) throws IOException, InterruptedException {
        Job job = getJob(id);
        if (job != null) {
            log.info("Job '{}'", job);
            log.info("Script command = '{}'", Arrays.toString(job.getJobType().getScript(job)));
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(job.getJobType().getScript(job));
                if (pipelineDir != null) {
                    processBuilder.directory(new File(pipelineDir));
                }
                Process process = processBuilder.start();
                log.debug("Command input stream '{}' \n Command error stream '{}'", IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8), IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
                int waitForCode = process.waitFor();
                if (process.exitValue() != 0) {
                    log.error("Failed to run job with exit code '{}' wait fore code '{}'. Command input stream '{}' \n Command error stream '{}'", process.exitValue(), waitForCode,
                            IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8), IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
                    return job;
                }
            } catch (IOException ioe) {
                log.error("There was an error when starting the restart operation for the job {}. {}", job, ioe.getMessage());
                throw ioe;
            } catch (InterruptedException ie) {
                log.error("An unexpected event occurred while waiting for the restart to complete for the job {}. {}", job, ie.getMessage());
                throw ie;
            }
        }
        return job;
    }

    public Job stopJob(String id) throws IOException, InterruptedException {
        Job job = getJob(id);
        if (job != null) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("flink", "cancel", id);
                Process process = processBuilder.start();
                log.info("Command input stream '{}' \n Command error stream '{}'", IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8), IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
                process.waitFor();
                if (process.exitValue() != 0) {
                    log.error("Failed to run job with exit code {}. Command output stream '{}' \n Command error stream '{}'", process.exitValue(),
                            IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8), IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8));
                    return job;
                }
            } catch (IOException ioe) {
                log.error("There was an error when starting the restart operation for the job {}. {}", job, ioe.getMessage());
                throw ioe;
            } catch (InterruptedException ie) {
                log.error("An unexpected event occurred while waiting for the restart to complete for the job {}. {}", job, ie.getMessage());
                throw ie;
            }
        }
        return job;
    }

    private int fillJobList(List<Job> jobs) throws IOException, InterruptedException {
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("flink", "list");
            process = processBuilder.start();
            jobs.addAll(readJobFromInputStream(process.getInputStream()));
            jobs.addAll(readJobFromInputStream(process.getErrorStream()));
            process.waitFor();
        } catch (IOException ioe) {
            log.error("There was an error when starting the `flink list` operations. {}", ioe.getMessage());
            throw ioe;
        } catch (InterruptedException ie) {
            log.error("An unexpected event occurred while waiting for the command to complete. {}", ie.getMessage());
            throw ie;
        }
        return process.exitValue();

    }

    private List<Job> readJobFromInputStream(InputStream inputStream) throws IOException {
        List<Job> jobs = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String stringLine;
            while ((stringLine = bufferedReader.readLine()) != null) {
                Matcher stringMatcher = pattern.matcher(stringLine);
                if (stringMatcher.matches()) {
                    String jobFullName = stringMatcher.group("jobFullName");
                    Job.JobType jobType = Utils.getEnumFromStringContains(jobFullName, Job.JobType.class, Job.JobType::getName);
                    if (jobType != null) {
                        Job job = Job.builder()
                                .jobId(JobID.fromHexString(stringMatcher.group("jobId")))
                                .jobIdString(stringMatcher.group("jobId"))
                                .jobFullName(jobFullName)
                                .jobType(jobType)
                                .jobState(JobStatus.valueOf(StringUtils.toRootUpperCase(stringMatcher.group("jobStatus"))))
                                .startTime(stringMatcher.group("date"))
                                .build();
                        setJobParameters(job, jobFullName);
                        jobs.add(job);
                    }
                }
            }
        } catch (IOException ioe) {
            log.error("Error on reading console output: {}", ioe.getMessage());
            throw ioe;
        }
        return jobs;
    }

    private void setJobParameters(Job job, String fullJobName) {
        String[] jobParameters = fullJobName.split("\\.");
        job.setJobBranch(jobParameters[0]);
        job.setJobPipeline(jobParameters[1]);
        if (job.getJobType() == Job.JobType.PROFILE || job.getJobType() == Job.JobType.GENERATOR || job.getJobType() == Job.JobType.PARSER) {
            job.setJobName(jobParameters[jobParameters.length - 1]);
        }
    }
}
