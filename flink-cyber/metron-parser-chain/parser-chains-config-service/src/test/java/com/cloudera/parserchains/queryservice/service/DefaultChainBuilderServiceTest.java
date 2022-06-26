package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.model.define.ParserName;
import com.cloudera.parserchains.core.utils.JSONUtils;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultChainBuilderServiceTest {
    static ParserName parserName = ParserName.of("Some Test Parser");
    private DefaultChainBuilderService service;

    @BeforeEach
    void beforeEach() {
        ChainBuilder chainBuilder = new DefaultChainBuilder(new ReflectiveParserBuilder(), new ClassIndexParserCatalog());
        service = new DefaultChainBuilderService(chainBuilder);
    }

    /**
     * {
     *     "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *     "name" : "My Parser Chain",
     *     "parsers" : [ {
     *       "id" : "8673f8f4-a308-4689-822c-0b01477ef378",
     *       "name" : "Timestamp",
     *       "type" : "com.cloudera.parserchains.parsers.TimestampParser",
     *       "config" : {
     *         "outputField" : {
     *           "outputField": "processing_time"
     *         }
     *       }
     *     }, {
     *       "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *       "name" : "Delimited Text",
     *       "type" : "com.cloudera.parserchains.parsers.DelimitedTextParser",
     *       "config" : {
     *         "inputField" : [ {
     *           "inputField": "original_string"
     *         }],
     *         "outputField" : [ {
     *           "fieldIndex" : "0",
     *           "fieldName" : "name"
     *         }, {
     *           "fieldIndex" : "1",
     *           "fieldName" : "address"
     *         }, {
     *           "fieldIndex" : "2",
     *           "fieldName" : "phone"
     *         }  ]
     *       }
     *     }]
     * }
     */
    @Multiline
    private String parserChain;

    @Test
    void success() throws InvalidParserException, IOException {
        ParserChainSchema schema = JSONUtils.INSTANCE.load(parserChain, ParserChainSchema.class);
        ChainLink head = service.build(schema);

        // validate
        Message input = Message.builder()
                .createdBy(LinkName.of("original", parserName))
                .addField(FieldName.of("original_string"), StringFieldValue.of("Homer Simpson, 740 Evergreen Terrace, (939)-555-0113"))
                .build();
        List<Message> results = head.process(input);

        assertThat("Expected 2 results; 1 from each parser in the chain.",
                results.size(), is(2));
        assertThat("Expected the message to have been labelled.",
                results.get(0).getCreatedBy().getLinkName(), is("8673f8f4-a308-4689-822c-0b01477ef378"));
        assertThat("Expected the timestamp to have been added.",
                results.get(0).getFields().keySet(), hasItem(FieldName.of("processing_time")));
        assertThat("Expected the message to have been labelled correctly.",
                results.get(1).getCreatedBy().getLinkName(), is("3b31e549-340f-47ce-8a71-d702685137f4"));
        assertThat("Expected the name field to have been added.",
                results.get(1).getFields().keySet(), hasItem(FieldName.of("name")));
        assertThat("Expected the address field to have been added.",
                results.get(1).getFields().keySet(), hasItem(FieldName.of("address")));
        assertThat("Expected the phone field to have been added.",
                results.get(1).getFields().keySet(), hasItem(FieldName.of("phone")));
    }

    /**
     * {
     *    "id":"1",
     *    "name":"Hello, Chain",
     *    "parsers":[
     *       {
     *          "name":"Route by Name",
     *          "type":"Router",
     *          "id":"96f5f340-5d96-11ea-89de-3b83ec1839cd",
     *          "config":{
     *          },
     *          "routing":{
     *             "routes":[
     *             ]
     *          }
     *       }
     *    ]
     * }
     */
    @Multiline
    private String missingMatchingField;

    @Test
    void error() throws IOException {
        ParserChainSchema schema = JSONUtils.INSTANCE.load(missingMatchingField, ParserChainSchema.class);
        assertThrows(InvalidParserException.class, () -> service.build(schema),
                "Expected exception because the router required a matching field to be defined.");
    }
}
