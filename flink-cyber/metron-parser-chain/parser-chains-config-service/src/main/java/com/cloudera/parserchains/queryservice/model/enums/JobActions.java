package com.cloudera.parserchains.queryservice.model.enums;

import org.apache.commons.lang3.StringUtils;

public enum JobActions {
    START(Constants.START_VALUE), STOP(Constants.STOP_VALUE), RESTART(Constants.RESTART_VALUE), UPDATE_CONFIG(Constants.UPDATE_CONFIG_VALUE), GET_CONFIG(Constants.GET_CONFIG_VALUE), STATUS(Constants.STATUS_VALUE);

    public final String action;

    JobActions(String action) {
        if (!StringUtils.equalsIgnoreCase(this.name(), action)) {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public static class Constants {
        private Constants() {
        }

        public static final String START_VALUE = "start";
        public static final String STOP_VALUE = "stop";
        public static final String RESTART_VALUE = "restart";
        public static final String UPDATE_CONFIG_VALUE = "update_config";
        public static final String GET_CONFIG_VALUE = "get_config";
        public static final String STATUS_VALUE = "status";
    }
}
