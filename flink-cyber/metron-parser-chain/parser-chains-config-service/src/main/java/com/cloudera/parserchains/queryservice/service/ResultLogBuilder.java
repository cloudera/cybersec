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

import com.cloudera.parserchains.queryservice.model.exec.ResultLog;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;

public class ResultLogBuilder {
    public static final String DEFAULT_SUCCESS_MESSAGE = "success";
    public static final String INFO_TYPE = "info";
    public static final String ERROR_TYPE = "error";

    /**
     * @return A builder that creates a result when a message is successfully parsed.
     */
    public static SuccessResultBuilder success() {
        return new SuccessResultBuilder();
    }

    /**
     * @return A builder that creates a result that indicates an error has occurred.
     */
    public static ErrorResultBuilder error() {
        return new ErrorResultBuilder();
    }

    /**
     * Private constructor.  Use {@link #success()} or {@link #error()}.
     */
    private ResultLogBuilder() {
        // do not use
    }

    public static class ErrorResultBuilder {
        private String parserId;
        private String parserName;
        private String message;
        private String type;
        private Throwable exception;

        public ErrorResultBuilder() {
            this.type = ERROR_TYPE;
        }

        public ErrorResultBuilder parserId(String parserId) {
          this.parserId = parserId;
          return this;
        }

        public ErrorResultBuilder parserName(String parserName) {
            this.parserName = parserName;
            return this;
        }

        public ErrorResultBuilder exception(Throwable e) {
            this.exception = e;
            return this;
        }

        public ErrorResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ResultLog build() {
            ResultLog resultLog = new ResultLog()
                .setParserId(parserId)
                .setParserName(parserName)
                .setType(type);
            if(exception != null) {
                resultLog.setMessage(getUsefulMessage(exception))
                         .setStackTrace(ExceptionUtils.getStackTrace(exception));
            } else {
              resultLog.setMessage(message);
            }
            return resultLog;
        }
    }

    public static class SuccessResultBuilder {
        private String parserId;
        private String parserName;
        private String message;
        private String type;

        public SuccessResultBuilder() {
            this.message = DEFAULT_SUCCESS_MESSAGE;
            this.type = INFO_TYPE;
        }

        public SuccessResultBuilder parserId(String parserId) {
            this.parserId = parserId;
            return this;
        }

        public SuccessResultBuilder parserName(String parserName) {
            this.parserName = parserName;
            return this;
        }

        public SuccessResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ResultLog build() {
            return new ResultLog()
                    .setParserId(parserId)
                    .setParserName(parserName)
                    .setMessage(message)
                    .setType(type);
        }
    }

    /**
     * Get a useful error message to display to the user.
     *
     * <p>The root cause exception does not always contain
     * a useful message. This traces backwards from the root
     * exception until it finds a useful error message.
     * @param t The exception.
     * @return
     */
    private static String getUsefulMessage(Throwable t) {
        List<Throwable> throwables = ExceptionUtils.getThrowableList(t);
        for(int i=throwables.size()-1; i>=0; i--) {
            Throwable cause = throwables.get(i);
            String message = removeClassName(cause.getMessage());
            if(StringUtils.isNotBlank(message)) {
                return message;
            }
        }

        String message = t.getMessage();
        if(StringUtils.isNotBlank(message)) {
            return message;
        } else {
            return t.getClass().getCanonicalName();
        }
    }

    /**
     * Removes a prepended class name from the beginning of the message.
     *
     * <p>When exceptions are wrapped, the class name of the original exception
     * is prepended to the error message. When returning the results to the user,
     * only the original error message, not the class name should be shown.
     *
     *  <pre>
     * removeClassName("IllegalArgumentException: Field name is required.") = "Field name is required."
     * removeClassName("Field name is required.") = "Field name is required."
     * </pre>
     * @param message The message to remove the class name from.
     * @return
     */
    private static String removeClassName(String message) {
        return RegExUtils.replaceFirst(message, "^\\w+:\\s*", "");
    }
}
