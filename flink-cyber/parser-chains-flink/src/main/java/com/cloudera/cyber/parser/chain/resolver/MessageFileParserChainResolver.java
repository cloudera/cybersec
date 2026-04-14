package com.cloudera.cyber.parser.chain.resolver;

import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.parser.ParserChainSource;
import com.cloudera.cyber.parser.TopicParserConfig;
import com.cloudera.cyber.parser.regex.resolver.PatternResolver;
import com.cloudera.cyber.parser.wrappers.MessageFileParser;

import java.nio.charset.StandardCharsets;

/**
 * Resolves the parser chain and source for a raw message containing a file name.
 */
public class MessageFileParserChainResolver extends ParserChainResolver {

    /**
     * Maps file patterns to parser chain and source.
     */
    private final PatternResolver<ParserChainSource> filePathResolver;

    /**
     * Constructor
     * <p>
     * Fails if one of the configured file regex strings fails compilation.
     *
     * @param parserConfig The parser configuration for the topic.
     * @param parser       The parser for topics containing file names.
     */
    public MessageFileParserChainResolver(TopicParserConfig parserConfig, MessageFileParser parser) {
        super(parser);
        this.filePathResolver = new PatternResolver<>(parserConfig.getFilePatternToParserMap());
    }

    /**
     * Get the parser chain and source for the raw message.
     *
     * @param message File path message to map.
     * @return The parser chain and source to use for this message.
     */
    @Override
    protected ParserChainSource getParserChainSource(MessageToParse message) {
        String filePath = new String(message.getOriginalBytes(), StandardCharsets.UTF_8);
        return filePathResolver.match(filePath, fp -> null);
    }

    /**
     * Returns the file path resolver for accessing header settings.
     *
     * @return the file path resolver
     */
    public PatternResolver<ParserChainSource> getFilePathResolver() {
        return filePathResolver;
    }
}
