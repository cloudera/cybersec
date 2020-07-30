package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.model.define.ParserSchema;
import com.cloudera.parserchains.queryservice.model.exec.ResultLog;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ResultLogBuilderTest {

    final static String parserId = "11c691cc-a141-43a0-b486-cb0e33bba820";
    final static String parserName = "Some Test Parser";
    final static String errorMessage = "This is the error to show to the user.";

    @Test
    void success() {
        ResultLog result = ResultLogBuilder.success()
                .parserId(parserId)
                .parserName(parserName)
                .build();
        assertThat(result.getMessage(), is(ResultLogBuilder.DEFAULT_SUCCESS_MESSAGE));
        assertThat(result.getParserId(), is(parserId));
        assertThat(result.getType(), is(ResultLogBuilder.INFO_TYPE));
    }

    @Test
    void successMessage() {
        final String message = "This is a custom success message";
        ResultLog result = ResultLogBuilder.success()
                .parserId(parserId)
                .message(message)
                .build();
        assertThat(result.getMessage(), is(message));
        assertThat(result.getParserId(), is(parserId));
        assertThat(result.getType(), is(ResultLogBuilder.INFO_TYPE));
    }

    @Test
    void error() {
        Exception error = new IllegalArgumentException(errorMessage);
        ResultLog result = ResultLogBuilder.error()
                .parserId(parserId)
                .exception(error)
                .build();
        assertThat(result.getMessage(), is(errorMessage));
        assertThat(result.getParserId(), is(parserId));
        assertThat(result.getType(), is(ResultLogBuilder.ERROR_TYPE));
    }

    @Test
    void wrappedExceptions1() {
        Exception error = new InvalidParserException(mock(ParserSchema.class),
                new IllegalArgumentException(errorMessage));
        ResultLog result = ResultLogBuilder.error()
                .parserId(parserId)
                .exception(error)
                .build();
        assertThat("Expected the message to come from the root cause exception.",
                result.getMessage(), is(errorMessage));
        assertThat(result.getParserId(), is(parserId));
        assertThat(result.getType(), is(ResultLogBuilder.ERROR_TYPE));
    }

    @Test
    void wrappedException2() {
        Exception error = new Exception(errorMessage, new IllegalArgumentException());
        ResultLog result = ResultLogBuilder.error()
                .parserId(parserId)
                .exception(error)
                .build();
        assertThat("Expected the message to come from the wrapper in this case.",
                result.getMessage(), is(errorMessage));
        assertThat(result.getParserId(), is(parserId));
        assertThat(result.getType(), is(ResultLogBuilder.ERROR_TYPE));
    }

    @Test
    void noDetailMessage() {
        Exception error = new NullPointerException();
        ResultLog result = ResultLogBuilder.error()
                .parserId(parserId)
                .exception(error)
                .build();
        assertThat(result.getParserId(), is(parserId));
        assertThat(result.getType(), is(ResultLogBuilder.ERROR_TYPE));
        assertThat("If there is no detail message to be found, use the class name.",
                result.getMessage(), is("java.lang.NullPointerException"));
    }

}
