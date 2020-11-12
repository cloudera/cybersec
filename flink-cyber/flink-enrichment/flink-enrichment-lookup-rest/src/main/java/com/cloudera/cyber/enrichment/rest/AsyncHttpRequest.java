package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.SingleValueEnrichment;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.List;

import static com.cloudera.cyber.DataQualityMessageLevel.ERROR;

@Slf4j
public class AsyncHttpRequest extends RichAsyncFunction<Message, Message> {

    public static final String REST_ENRICHMENT_FEATURE = "rest";
    public static final String REST_REQUEST_FAILED_QUALITY_MESSAGE = "Rest request url='%s' entity='%s' failed '%s'";
    public static final String ANY_SOURCE_NAME = "ANY";

    private final @NonNull RestEnrichmentConfig config;
    private final boolean matchAnySource;

    private transient SingleValueEnrichment enrichment;

    /**
     * Asynchronously loads rest maps into cache.
     */
    private transient RestRequest request;

    public AsyncHttpRequest(@NonNull RestEnrichmentConfig config) {
        this.config = config;
        this.matchAnySource = config.getSources().contains(ANY_SOURCE_NAME);
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        log.debug("Opening AsyncHttpRequest {}", config.toString());
        // setup the client
        request = config.createRestEnrichmentRequest();
        enrichment = new SingleValueEnrichment(config.getPrefix(), REST_ENRICHMENT_FEATURE);
    }

    @Override
    public void close() {
        if (request != null) {
            try {
                request.close();
            } catch (IOException e) {
                log.error("Exception while closing rest request.", e);
            }
        }
    }

    private List<DataQualityMessage> createDataQualityMessages(List<String> errorMessages) {
        if (!errorMessages.isEmpty()) {
            List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
            errorMessages.forEach((errorMessage) -> enrichment.addQualityMessage(dataQualityMessages, ERROR, errorMessage));
            return dataQualityMessages;
         } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void asyncInvoke(Message message, ResultFuture<Message> resultFuture) {
        List<String> configSource = config.getSources();
        String messageSource = message.getSource();
        if (matchAnySource || configSource.contains(messageSource)) {
            request.getResult(!matchAnySource, message.getExtensions()).handleAsync((RestRequestResult restRequestResult, Throwable e) -> {
                if (restRequestResult == null) {
                    restRequestResult = new RestRequestResult();
                }
                if (e != null) {
                    restRequestResult.getErrors().add(e.getMessage());
                }
                Collection<Message> result = Collections.singleton(MessageUtils.enrich(message, restRequestResult.getExtensions(), config.getPrefix(), createDataQualityMessages(restRequestResult.getErrors())));
                log.debug("Returned model result {}", result);
                resultFuture.complete(result);
                return restRequestResult;
            });
        } else {
            // enrichment not relevant for this source - pass message through
            log.debug("predicate returned false or event source {} does not match rest source {}", messageSource, configSource);
            resultFuture.complete(Collections.singleton(message));
        }
   }
}
