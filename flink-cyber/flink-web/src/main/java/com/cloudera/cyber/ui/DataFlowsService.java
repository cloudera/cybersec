package com.cloudera.cyber.ui;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DataFlowsService {

    private Map<String, FlinkDataFlowYarn> flows = new HashMap<>();

    public void addFlow(String name, URL jar, Properties config) {
        // find the program files
        flows.put(name, new FlinkDataFlowYarn(name, jar, config));
    }

    public void startFlow(String name) throws Exception {
        flows.get(name).start();
    }

    public void stopFlow(String name) throws Exception {
        flows.get(name).stop();
    }

    public void restartFlow(String name, Properties config, URL jar) throws Exception {
        flows.get(name).restart(config, jar);
    }
}
