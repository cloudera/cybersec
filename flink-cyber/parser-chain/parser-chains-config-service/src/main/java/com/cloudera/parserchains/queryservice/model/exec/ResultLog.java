package com.cloudera.parserchains.queryservice.model.exec;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Describes the result of parsing a single message.
 *
 * <p>See also {@link ChainTestRequest} which is the top-level class for the
 * data model used for the "Live View" feature.
 */
public class ResultLog {

    private String type;
    private String message;
    private String parserId;
    private String parserName;
    private String stackTrace;

    public ResultLog setType(String type) {
        this.type = type;
        return this;
    }

    public ResultLog setMessage(String message) {
        this.message = message;
        return this;
    }

    public ResultLog setParserId(String parserId) {
        this.parserId = parserId;
        return this;
    }

    public ResultLog setParserName(String parserName) {
        this.parserName = parserName;
        return this;
    }

    public ResultLog setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getParserId() {
      return parserId;
    }

    public String getParserName() {
        return parserName;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResultLog resultLog = (ResultLog) o;
        return new EqualsBuilder()
                .append(type, resultLog.type)
                .append(message, resultLog.message)
                .append(parserId, resultLog.parserId)
                .append(parserName, resultLog.parserName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(type)
                .append(message)
                .append(parserId)
                .append(parserName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ResultLog{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", parserId='" + parserId + '\'' +
                ", parserName='" + parserName + '\'' +
                '}';
    }

}
