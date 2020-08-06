/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.parserchains.queryservice.model.exec;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

/**
 * The result of parsing a message with a parser chain or parser.
 */
public class ParserResult {
    /**
     * The input fields.
     */
    private Map<String, String> input;

    /**
     * The output fields produced.
     */
    private Map<String, String> output;

    /**
     * Describes the outcome of parsing the message.
     */
    private ResultLog log;

    /**
     * The individual parser results; one for each parser in the chain.
     * <p>If no results, this field should not be shown when serialized to JSON.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<ParserResult> parserResults;

    public ParserResult() {
        this.input = new HashMap<>();
        this.output = new HashMap<>();
        this.parserResults = new ArrayList<>();
    }

    public Map<String, String> getInput() {
        return input;
    }

    public ParserResult setInput(Map<String, String> input) {
        this.input = input;
        return this;
    }

    public ParserResult addInput(String fieldName, String fieldValue) {
        this.input.put(fieldName, fieldValue);
        return this;
    }

    public Map<String, String> getOutput() {
        return output;
    }

    public ParserResult setOutput(Map<String, String> output) {
        this.output = output;
        return this;
    }

    public ParserResult addOutput(String fieldName, String fieldValue) {
        this.output.put(fieldName, fieldValue);
        return this;
    }

    public ResultLog getLog() {
        return log;
    }

    public ParserResult setLog(ResultLog log) {
        this.log = log;
        return this;
    }

    public List<ParserResult> getParserResults() {
        return parserResults;
    }

    public ParserResult addParserResult(ParserResult result) {
        parserResults.add(result);
        return this;
    }

    public void setParserResults(List<ParserResult> parserResults) {
        this.parserResults = parserResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParserResult that = (ParserResult) o;
        return Objects.equals(input, that.input) &&
                Objects.equals(output, that.output) &&
                Objects.equals(log, that.log) &&
                Objects.equals(parserResults, that.parserResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output, log, parserResults);
    }

    @Override
    public String toString() {
        return "ParserResult{" +
                "input=" + input +
                ", output=" + output +
                ", log=" + log +
                ", chainResults=" + parserResults +
                '}';
    }
}
