package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.parser.ParserChainMap;
import com.cloudera.cyber.parser.ParserChainSource;
import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parse raw text containing a single message.
 */
@Slf4j
public class SingleMessageParser implements ParserInterface {
    public static final String CHAIN_PARSER_FEATURE = "chain_parser";
    public static final String TIMESTAMP_NOT_EPOCH = "Timestamp is not in epoch milliseconds or seconds. ";
    public static final String NO_TIMESTAMP_FIELD_MESSAGE = "Message does not contain a timestamp field.";
    public static final byte[] EMPTY_SIGNATURE = new byte[1];

    /**
     * Maps parser chain name to parser chain definition.
     */
    private final HashMap<String, ChainLink> chains;
    /**
     * Signature for getting raw text crypto signature.
     * May be null if messages are not signed.
     */
    private final Signature signature;

    /**
     * The parser chain implementation.
     */
    private final DefaultChainRunner chainRunner;

    /**
     * Constructor
     *
     * @param chains         Map from chain name to chain parser.
     * @param signature      Signature for signing message content or null if no signature required.
     */
    private SingleMessageParser(HashMap<String, ChainLink> chains, Signature signature) {
        this.chains = chains;
        this.signature = signature;
        this.chainRunner = new DefaultChainRunner();
        log.info("Parser chains {}", this.chains);
    }

    public static SingleMessageParser create(ParserChainMap parserChainMap, PrivateKey signKey) throws InvalidParserException, NoSuchAlgorithmException, InvalidKeyException {
       return new SingleMessageParser(buildChainMap(parserChainMap), createSignature(signKey));
    }

    private static HashMap<String, ChainLink> buildChainMap(ParserChainMap parserChainMap) {

        HashMap<String, ChainLink> chains = new HashMap<>();
        ChainBuilder chainBuilder = new DefaultChainBuilder(new ReflectiveParserBuilder(),
                new ClassIndexParserCatalog());

        String chainsWithErrors = null;
        for (Map.Entry<String, ParserChainSchema> entry : parserChainMap.entrySet()) {
            String chainName = entry.getKey();
            try {
                chains.put(chainName, chainBuilder.build(entry.getValue()));
            } catch (InvalidParserException e) {
                log.error("Cannot build parser chain '{}'", chainName, e);
                if (chainsWithErrors == null) {
                    chainsWithErrors = chainName;
                } else {
                    chainsWithErrors = chainsWithErrors.concat(", ").concat(chainName);
                }
            }
        }

        if (chainsWithErrors != null) {
            throw new IllegalArgumentException(String.format("The following parser chains did not parse: %s", chainsWithErrors));
        }

        return chains;
    }

    private static Signature createSignature(PrivateKey signKey) throws NoSuchAlgorithmException, InvalidKeyException {
        Signature signature = null;

        if (signKey != null) {
            signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(signKey);
        }
        return signature;
    }

    /**
     * Parse the raw message and output it.
     * If message contains data quality errors, output to the error side output.
     * Otherwise, send to the output collector.
     *
     * @param parserChainSource The chain parser to use for parsing the message.
     * @param message           The raw message to be parsed.
     * @param output            Sends parsed messages on to next processor.
     */
    public void parse(ParserChainSource parserChainSource, MessageToParse message, AbstractParserOutput output) {
        final ChainLink chain = chains.get(parserChainSource.getChainKey());

        final List<com.cloudera.parserchains.core.Message> run = chainRunner.run(message, chain);
        final com.cloudera.parserchains.core.Message m = run.get(run.size() - 1);
        if (m.getEmit()) {
            Optional<String> errorMessage = m.getError().map(Throwable::getMessage);
            long messageTimestamp = Instant.now().toEpochMilli();

            if (errorMessage.isEmpty()) {
                Optional<FieldValue> timestamp = m.getField(FieldName.of("timestamp"));

                if (timestamp.isPresent()) {
                    try {
                        // handle timestamps that are <seconds>.<milliseconds>
                        String timestampString = timestamp.get().get().replace(".", "");
                        messageTimestamp = Long.parseLong(timestampString);
                        if (timestampString.length() < 12) {
                            // normalize second times
                            messageTimestamp *= 1000;
                        }
                    } catch (NumberFormatException nfe) {
                        errorMessage = Optional.of(TIMESTAMP_NOT_EPOCH.concat(nfe.getMessage()));
                    }
                } else {
                    errorMessage = Optional.of(NO_TIMESTAMP_FIELD_MESSAGE);
                }
            }

            List<DataQualityMessage> dataQualityMessages = errorMessage.
                    map(messageText -> Collections.singletonList(
                            DataQualityMessage.builder().
                                    field(this.chainRunner.getInputField().get()).
                                    feature(CHAIN_PARSER_FEATURE).
                                    level(DataQualityMessageLevel.ERROR.name()).
                                    message(messageText).
                                    build())).
                    orElse(null);

            Message parsedMessage = Message.builder().extensions(fieldsFromChain(errorMessage.isPresent(), m.getFields()))
                    .source(parserChainSource.getSource())
                    .originalSource(SignedSourceKey.builder()
                            .topic(message.getTopic())
                            .partition(message.getPartition())
                            .offset(message.getOffset())
                            .signature(signOriginalText(m))
                            .build())
                    .ts(messageTimestamp)
                    .dataQualityMessages(dataQualityMessages)
                    .build();

            output.outputMessage(parsedMessage);
        }
    }

    /**
     * Return the crypto signature of the original bytes in the message.
     *
     * @param m Message to sign.
     * @return Signature of the message or empty signature if the message is not signed.
     */
    private byte[] signOriginalText(com.cloudera.parserchains.core.Message m) {

        if (signature != null) {
            Optional<FieldValue> originalMessage = m.getField(this.chainRunner.getInputField());
            if (originalMessage.isPresent()) {
                byte[] bytes = originalMessage.get().toBytes();
                try {
                    signature.update(bytes);
                    return signature.sign();
                } catch (SignatureException e) {
                    // this should not happen because signature is initialized but log it just in case
                    log.error("Failed to sign message.", e);
                }
            }
        }
        return EMPTY_SIGNATURE;
    }

    /**
     * Map the field names produced by the parser to an extension map.
     * If the message parsed correctly, remove the original text field because it is redundant.
     * If the message has an error, leave the original text so it can be reprocessed or debugged.
     *
     * @param hasError True if the message has an error.  False otherwise.
     * @param fields   The fields of the message.
     * @return The extension map.
     */
    private Map<String, String> fieldsFromChain(boolean hasError, Map<FieldName, FieldValue> fields) {
        final String inputFieldName = this.chainRunner.getInputField().get();
        return fields.entrySet().stream().filter(mapEntry -> {
            String fieldName = mapEntry.getKey().get();
            return ((!StringUtils.equals(fieldName, inputFieldName) && !fieldName.startsWith(Constants.METADATA_PREFIX)) || hasError) && !fieldName.equals("timestamp");
        }).collect(Collectors.toMap(entryMap -> entryMap.getKey().get(), entryMap -> entryMap.getValue().get()));
    }

}
