package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Host for the chain parser jobs
 */
@Slf4j
public abstract class ParserJob {

    protected static final String PARAM_CHAIN_CONFIG = "chain";
    protected static final String PARAM_CHAIN_CONFIG_FILE = "chain.file";
    protected static final String PARAM_CHAIN_CONFIG_DIRECTORY = "chain.dir";
    protected static final List<String> PARAM_CHAIN_CONFIG_EXCLUSIVE_LIST = Arrays.asList(
            PARAM_CHAIN_CONFIG, PARAM_CHAIN_CONFIG_FILE, PARAM_CHAIN_CONFIG_DIRECTORY);

    protected static final String PARAM_TOPIC_MAP_CONFIG = "chain.topic.map";
    protected static final String PARAM_TOPIC_MAP_CONFIG_FILE = "chain.topic.map.file";
    public static final String PARAM_PRIVATE_KEY_FILE = "key.private.file";
    public static final String PARAM_PRIVATE_KEY = "key.private.base64";
    public static final String ERROR_MESSAGE_SIDE_OUTPUT = "error-message";
    public static final String SIGNATURE_ENABLED = "signature.enabled";
    public static final String PARAM_STREAMING_ENRICHMENTS_CONFIG = "chain.enrichments.file";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);
        validateParams(params);

        String chainConfig = readConfigChainMap(PARAM_CHAIN_CONFIG_FILE, PARAM_CHAIN_CONFIG, PARAM_CHAIN_CONFIG_DIRECTORY, params, null);
        String topicConfig = readConfigMap(PARAM_TOPIC_MAP_CONFIG_FILE, PARAM_TOPIC_MAP_CONFIG, params, "{}");


        ParserChainMap chainSchema = JSONUtils.INSTANCE.load(chainConfig, ParserChainMap.class);
        TopicPatternToChainMap topicMap = JSONUtils.INSTANCE.load(topicConfig, TopicPatternToChainMap.class);
        String defaultKafkaBootstrap = params.get(Utils.KAFKA_PREFIX + ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);

        String enrichmentsConfigFile = params.get(PARAM_STREAMING_ENRICHMENTS_CONFIG);
        List<String> streamingSourcesProduced = Collections.emptyList();
        StreamingEnrichmentsSourceFilter streamingEnrichFilter = null;
        EnrichmentsConfig streamingEnrichmentsConfig = null;
        if (enrichmentsConfigFile != null) {
            streamingEnrichmentsConfig = EnrichmentsConfig.load(enrichmentsConfigFile);

            List<String> sourcesProduced = topicMap.getSourcesProduced();
            streamingSourcesProduced = streamingEnrichmentsConfig.getStreamingEnrichmentSources().stream().
                    filter(sourcesProduced::contains).collect(Collectors.toList());
            if (!streamingSourcesProduced.isEmpty()) {
                streamingEnrichFilter = new StreamingEnrichmentsSourceFilter(streamingSourcesProduced);
            }
        }

        DataStream<MessageToParse> source = createSource(env, params, topicMap);

        PrivateKey privateKey = null;
        if (params.getBoolean(SIGNATURE_ENABLED, true)) {
            byte[] privKeyBytes = params.has(PARAM_PRIVATE_KEY_FILE) ?
                    Files.readAllBytes(Paths.get(params.get(PARAM_PRIVATE_KEY_FILE))) :
                    Base64.getDecoder().decode(params.getRequired(PARAM_PRIVATE_KEY));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
            privateKey = keyFactory.generatePrivate(privSpec);
        }

        SingleOutputStreamOperator<Message> results =
                source.process(new ChainParserMapFunction(chainSchema, topicMap, privateKey, defaultKafkaBootstrap))
                        .name("Parser " + source.getTransformation().getName()).uid("parser" + source.getTransformation().getUid());
        final OutputTag<Message> errorMessageSideOutput = new OutputTag<Message>(ERROR_MESSAGE_SIDE_OUTPUT) {
        };
        DataStream<Message> errorMessages = results.getSideOutput(errorMessageSideOutput);

        writeResults(params, results);
        writeOriginalsResults(params, source);

        if (streamingEnrichFilter != null) {
            DataStream<Message> streamingEnrichmentMessages = results.filter(streamingEnrichFilter).name("Streaming Enrichment Sources Filter");
            SingleOutputStreamOperator<EnrichmentCommand> enrichmentCommands = streamingEnrichmentMessages.process(new MessageToEnrichmentCommandFunction(streamingSourcesProduced, streamingEnrichmentsConfig)).name("Message to EnrichmentCommand");
            errorMessages = errorMessages.union(enrichmentCommands.getSideOutput(errorMessageSideOutput));
            writeEnrichments(params, enrichmentCommands, streamingSourcesProduced, streamingEnrichmentsConfig);
        }

        writeErrors(params, errorMessages);

        return env;
    }

    private void validateParams(ParameterTool params) {
        //Check for mutually exclusive chain params
        final long chainParamAmount = PARAM_CHAIN_CONFIG_EXCLUSIVE_LIST.stream()
                .filter(params::has)
                .count();
        if (chainParamAmount > 1) {
            throw new RuntimeException("It's not allowed to provide more than one chain param! " +
                    "Select one of the following: " + PARAM_CHAIN_CONFIG_EXCLUSIVE_LIST);
        }
    }

    private String readConfigChainMap(String fileParamKey, String inlineConfigKey, String directoryConfigKey,
                                      ParameterTool params, String defaultConfig) throws IOException {
        if (params.has(directoryConfigKey)) {
            final ParserChainMap result = new ParserChainMap();

            final Path path = new Path(params.getRequired(directoryConfigKey));
            final FileSystem fileSystem = path.getFileSystem();

            final FileStatus[] fileStatusList = fileSystem.listStatus(path);
            if (ArrayUtils.isEmpty(fileStatusList)){
                throw new RuntimeException(String.format("Provided config directory doesn't exist or empty [%s]!", path));
            }
            for (FileStatus fileStatus : fileStatusList) {
                final Path filePath = fileStatus.getPath();
                if (filePath.getName().endsWith(".json")) {
                    final ParserChainSchema chainSchema;

                    try (FSDataInputStream fsDataInputStream = fileSystem.open(filePath)) {
                        final String chainString = IOUtils.toString(fsDataInputStream, StandardCharsets.UTF_8);
                        chainSchema = JSONUtils.INSTANCE.load(chainString, ParserChainSchema.class);
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("Wasn't able to read the chain file [%s]!", filePath));
                    }

                    final String schemaName = chainSchema.getName();
                    if (result.containsKey(schemaName)) {
                        throw new RuntimeException(String.format("Found a duplicate schema named [%s] in the [%s] file, which isn't allowed!", schemaName, filePath));
                    }
                    result.put(schemaName, chainSchema);
                }
            }
            return JSONUtils.INSTANCE.toJSON(result, false);
        }
        return readConfigMap(fileParamKey, inlineConfigKey, params, defaultConfig);
    }

    private String readConfigMap(String fileParamKey, String inlineConfigKey, ParameterTool params,
                                 String defaultConfig) throws IOException {
        if (params.has(fileParamKey)) {
            return new String(Files.readAllBytes(Paths.get(params.getRequired(fileParamKey))), StandardCharsets.UTF_8);
        }
        return defaultConfig == null ? params.getRequired(inlineConfigKey)
                : params.get(inlineConfigKey, defaultConfig);

    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results);

    protected abstract void writeEnrichments(ParameterTool params, DataStream<EnrichmentCommand> streamingEnrichmentResults, List<String> streamingEnrichmentSources, EnrichmentsConfig streamingEnrichmentConfig);

    protected abstract void writeErrors(ParameterTool params, DataStream<Message> errors);

    protected abstract DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params,
                                                               TopicPatternToChainMap topicPatternToChainMap);

}
