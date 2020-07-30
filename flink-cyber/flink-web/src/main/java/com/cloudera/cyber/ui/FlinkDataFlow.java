package com.cloudera.cyber.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public interface FlinkDataFlow {

    /**
     * @return name for the flow, injected to the Flink application
     */
    String getName();

    /**
     * @return URL to the REST API for the Flink cluster
     */
    URL getRestUrl() throws MalformedURLException;

    Properties getConfig();
    void setConfig(Properties config);

    /**
     * The flink Job ID for reference
     *
     * @return jobId string, a uuid format
     */
    String getJobId();

    boolean isRunning();

    /**
     * Start a new flink data flow
     *
     * @param savepoint Location of a savepoint to bootstrap from
     * @throws Exception
     */
    void start(String savepoint) throws Exception;
    void start() throws Exception;

    /**
     * Stop the application
     *
     * @param checkpoint Path to create a save point
     * @throws Exception
     */
    void stop(String checkpoint) throws Exception;
    void stop() throws Exception;

    /**
     * Create a savepoint for the application
     *
     * @param savepoint Path to create a save point
     * @throws Exception
     */
    void savepoint(String savepoint) throws Exception;

    /**
     * Savepoint and stop application, restart with new jar and configs
     *
     * @param config Properties to send to the flow
     * @throws Exception
     */
    void restart(Properties config, URL jar) throws Exception;
}
