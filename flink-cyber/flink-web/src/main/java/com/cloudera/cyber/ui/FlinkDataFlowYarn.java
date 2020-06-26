package com.cloudera.cyber.ui;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class FlinkDataFlowYarn implements FlinkDataFlow {

    @Value("#{cyber.flink.savepointBase}")
    private String savepointBase;

    @Value("#{cyber.flink.flinkCli}")
    private String flinkCommand;

    @Value("#{cyber.flink.restBase}")
    private String flinkRestBase;

    public enum FlinkCommand {
        START("start"),
        STOP("cancel"),
        SAVEPOINT("savepoint");

        private String command;
        FlinkCommand(String command) {
            this.command = command;
        }
        protected String getCommand() {
            return this.command;
        }
    }

    public FlinkDataFlowYarn(String name, URL jar, Properties config) {
        this.name = name;
        this.jar = jar;
        this.config = config;
    }

    private String name;
    private String jobId;
    private Properties config;
    private URL jar;

    /**
     *
     * @param command
     * @param flinkArgs
     * @return JobID for the submitted job
     * @throws IOException
     * @throws InterruptedException
     */
    private String flinkYarnCmd(FlinkCommand command, List<String> flinkArgs) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> args = new ArrayList<String>();

        args.add(command.getCommand());
        args.add("-m");
        args.add("yarn-cluster");

        // TODO add memory parameters
        if (command == FlinkCommand.START) {
            args.addAll(flinkArgs);

            args.add(jar.toString());
            // add the parameters
            args.addAll(
                    this.config.entrySet().stream()
                            .flatMap(e -> Stream.of("-D", String.format("%s=%s", e.getKey(), e.getValue())))
                            .collect(Collectors.toList()));
        }



        Process process = builder.command(flinkCommand).start();

        // extract the job id
        String jobId = "";

        int exitCode = process.waitFor();
        return jobId;

    }

    public URL getRestUrl() throws MalformedURLException {
        return new URL(String.format("%s/jobs/%s", flinkRestBase, getJobId()));
    }

    public boolean isRunning() {
        return false;
    }

    public void start(String savepoint) throws Exception {
        List<String> flinkArgs = new ArrayList<String>();

        if (savepoint != null) {
            flinkArgs.add("-d");
            flinkArgs.add("-s");
            flinkArgs.add(String.format("%s/%s", savepointBase, savepoint));
        }

        List<String> args = new ArrayList<String>();
        flinkYarnCmd(FlinkCommand.START, flinkArgs);
    }

    public void stop(String savepoint) throws Exception {
        List<String> flinkArgs = new ArrayList<String>();

        if (savepoint != null) {
            flinkArgs.add("-s");
            flinkArgs.add(String.format("%s/%s", savepointBase, savepoint));
        }

        flinkArgs.add(getJobId());
        flinkYarnCmd(FlinkCommand.STOP, flinkArgs);
    }

    public void savepoint(String savepoint) throws Exception {
        flinkYarnCmd(FlinkCommand.SAVEPOINT, Arrays.asList(
            getJobId(),
            String.format("%s/%s", savepointBase, savepoint)
        ));
    }

    public void restart(Properties config, URL jar) throws Exception {
        String savepoint = String.format("%s/%s", getJobId(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        stop(savepoint);
        setJar(jar);
        setConfig(config);
        start(savepoint);
    }
}
