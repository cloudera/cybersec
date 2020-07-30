package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.catalog.ParserCatalog;
import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.model.define.ParserSchema;
import com.cloudera.parserchains.core.model.define.RouteSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

public class DefaultChainBuilder implements ChainBuilder {
    private static final Logger logger = LogManager.getLogger(DefaultChainBuilder.class);
    private ParserBuilder parserBuilder;
    private ParserCatalog parserCatalog;

    public DefaultChainBuilder(ParserBuilder parserBuilder, ParserCatalog parserCatalog) {
        this.parserBuilder = parserBuilder;
        this.parserCatalog = parserCatalog;
    }

    @Override
    public ChainLink build(ParserChainSchema chainSchema) throws InvalidParserException {
        return doBuild(chainSchema);
    }

    private ChainLink doBuild(ParserChainSchema chainSchema) throws InvalidParserException {
        List<ParserInfo> parserInfos = parserCatalog.getParsers();

        // build the chain
        ChainLink head = null;
        ChainLink current = null;
        for(ParserSchema parserSchema: chainSchema.getParsers()) {
            ChainLink next;
            boolean isRouter = ParserID.router().equals(parserSchema.getId());
            if(isRouter) {
                next = buildRouter(parserSchema);
            } else {
                next = buildLink(parserSchema, parserInfos);
            }

            if(head == null) {
                head = next;
                current = next;
            } else {
                current.setNext(next);
                current = next;
            }
        }

        return head;
    }

    /**
     * Build a link in a parser chain.
     * @param parserSchema Defines a link in the chain.
     * @param parserInfos The known set of parsers.
     * @return The next link in the chain.
     */
    private NextChainLink buildLink(ParserSchema parserSchema, List<ParserInfo> parserInfos)
            throws InvalidParserException {
        try {
            LinkName linkName = LinkName.of(parserSchema.getLabel(), parserSchema.getName());
            Parser parser = buildParser(parserSchema, parserInfos);
            return new NextChainLink(parser, linkName);

        } catch(Exception e) {
            // throw a checked exception so that we know which parser is the cause of the problem
            throw new InvalidParserException(parserSchema, e);
        }
    }

    /**
     * Builds a router in a parser chain.
     * @param routerSchema Defines a router.
     * @return A router that is part of a parser chain.
     */
    private RouterLink buildRouter(ParserSchema routerSchema) throws InvalidParserException {
        RouterLink routerLink;
        try {
            // build the router
            FieldName inputField = FieldName.of(routerSchema.getRouting().getMatchingField());
            routerLink = new RouterLink()
                    .withInputField(inputField);

            // define the router's routes
            for (RouteSchema routeSchema : routerSchema.getRouting().getRoutes()) {
                ChainLink subChain = doBuild(routeSchema.getSubChain());
                if (routeSchema.isDefault()) {
                    routerLink.withDefault(subChain);
                } else {
                    Regex regex = Regex.of(routeSchema.getMatchingValue());
                    routerLink.withRoute(regex, subChain);
                }
            }
        } catch(Exception e) {
            // throw a checked exception so that we know which parser is the cause of the problem
            throw new InvalidParserException(routerSchema, e);
        }
        return routerLink;
    }

    /**
     * Builds a {@link Parser} given a {@link ParserSchema}.
     * @param parserSchema Defines the parser to build.
     * @param parserInfos A list of information about all known parsers.
     * @return A {@link Parser}.
     */
    private Parser buildParser(ParserSchema parserSchema,
                               List<ParserInfo> parserInfos) throws InvalidParserException {
        String className = parserSchema.getId().getId();
        Optional<ParserInfo> parserInfo = parserInfos
                .stream()
                .filter(info -> className.equals(info.getParserClass().getCanonicalName()))
                .findFirst();
        if(parserInfo.isPresent()) {
            return parserBuilder.build(parserInfo.get(), parserSchema);

        } else {
            String error = String.format("Unable to find parser in catalog; class=%s", className);
            logger.error(error);
            throw new InvalidParserException(parserSchema, error);
        }
    }

}
