package com.cloudera.service.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    private JobID jobId;

    private String jobIdString;

    private String jobFullName;

    private String confName;

    private JobStatus jobState;

    private String jobBranch;

    private String jobPipeline;

    private String startTime;

    private JobType jobType;

    private String user;

    @AllArgsConstructor
    @Getter
    public enum JobType {
        GENERATOR("generate", "cs-restart-generator", "."),
        PARSER("parse", "cs-restart-parser", "."),
        TRIAGE("triage", "cs-restart-triage", "."),
        PROFILE("profile", "cs-restart-profile", "."),
        INDEX("index", "cs-restart-index", ".");

        private final String name;
        private final String scriptName;
        private final String nameDelimiter;

        public String[] getScript(Job job) {
            switch (this) {
                case PROFILE:
                case PARSER:
                    return new String[]{scriptName, job.getJobBranch(), job.getJobPipeline(), job.getConfName()};
                case INDEX:
                case TRIAGE:
                    return new String[]{scriptName, job.getJobBranch(), job.getJobPipeline()};
            }
            return new String[]{};
        }

    }
}
