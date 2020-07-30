package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.ChainRunner;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.queryservice.model.exec.ParserResult;
import com.cloudera.parserchains.queryservice.model.exec.ResultLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.cloudera.parserchains.queryservice.service.ResultLogBuilder.error;
import static com.cloudera.parserchains.queryservice.service.ResultLogBuilder.success;

@Service
public class DefaultChainExecutorService implements ChainExecutorService {
    private static final Logger logger = LogManager.getLogger(DefaultChainExecutorService.class);
    private ChainRunner chainRunner;

    public DefaultChainExecutorService(ChainRunner chainRunner) {
        this.chainRunner = chainRunner;
    }

    @Override
    public ParserResult execute(ChainLink chain, String textToParse) {
        Message original = chainRunner.originalMessage(textToParse);
        try {
            if (chain != null) {
                List<Message> messages = chainRunner.run(textToParse, chain);
                return chainExecuted(messages);
            } else {
                return chainNotDefined(original);
            }
        } catch(Throwable e) {
            return chainFailed(original, e);
        }
    }

    /**
     * Returns a {@link ParserResult} after a parser chain was executed.
     * @param messages The result of executing the parser chain.
     */
    private ParserResult chainExecuted(List<Message> messages) {
        ParserResult result = new ParserResult();

        // define the input fields for the parser chain
        Message input = messages.get(0);
        result.setInput(input.getFields()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().get(),
                        e -> e.getValue().get())));

        // define the fields output by the parser chain
        Message output = messages.get(messages.size()-1);
        result.setOutput(output.getFields()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().get(),
                        e -> e.getValue().get())));

        // define the log section
        result.setLog(buildResultLog(output));

        // define the parser-by-parser set of results
        List<ParserResult> parserResults = buildParserByParserResults(messages);
        result.setParserResults(parserResults);

        return result;
    }

    private ResultLog buildResultLog(Message output) {
        String parserId = output.getCreatedBy().getLinkName();
        String parserName = output.getCreatedBy().getParserName().getName();
        return output.getError()
                    .map(e -> error()
                            .parserId(parserId)
                            .parserName(parserName)
                            .exception(e)
                            .build())
                    .orElseGet(() -> success()
                            .parserId(parserId)
                            .parserName(parserName)
                            .build());
    }

    private List<ParserResult> buildParserByParserResults(List<Message> messages) {
        List<ParserResult> results = new ArrayList<>();

        for (int i = 0; i < messages.size() - 1; i++) {
            ParserResult result = new ParserResult();

            // define the input fields
            Message input = messages.get(i);
            result.setInput(input.getFields()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().get(),
                            e -> e.getValue().get())));

            // define the output fields
            Message output = messages.get(i + 1);
            result.setOutput(output.getFields()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().get(),
                            e -> e.getValue().get())));

            // define the log section
            ResultLog resultLog = buildResultLog(output);
            result.setLog(resultLog);

            results.add(result);
        }
        return results;
    }

    /**
     * Return a {@link ParserResult} indicating that an unexpected error occurred
     * while executing the parser chain.
     * @param original The original message to parse.
     */
    private ParserResult chainFailed(Message original, Throwable t) {
        logger.info("There was a problem executing the parser chain.", t);
        ParserResult result = new ParserResult();

        // define the input fields
        result.setInput(original.getFields()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().get(),
                        e -> e.getValue().get())));

        // there are no output fields
        // define the log section
        ResultLog log = ResultLogBuilder.error()
                .parserId(original.getCreatedBy().getLinkName())
                .parserName(original.getCreatedBy().getParserName().getName())
                .exception(t)
                .build();
        return result.setLog(log);
    }

    /**
     * Return a {@link ParserResult} indicating that no parser chain has yet been
     * defined.  For example, there are no parsers in the chain.
     * <p>If a parser chain has not yet been defined by the user, the result returned
     * should indicate success even though we could not parse anything.
     * @param original The original message to parse.
     */
    private ParserResult chainNotDefined(Message original) {
        ParserResult result = new ParserResult();

        // define the input fields
        result.setInput(original.getFields()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().get(),
                        e -> e.getValue().get())));

        // there are no output fields
        // define the log section
        ResultLog log = ResultLogBuilder.success()
                .parserId(original.getCreatedBy().getLinkName())
                .parserName(original.getCreatedBy().getParserName().getName())
                .message("No parser chain defined.")
                .build();
        return result.setLog(log);
    }
}
