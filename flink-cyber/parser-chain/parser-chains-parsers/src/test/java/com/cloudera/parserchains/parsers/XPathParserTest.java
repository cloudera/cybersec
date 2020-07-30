package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.Message;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class XPathParserTest {
    private XPathParser parser;

    @BeforeEach
    void beforeEach() {
        parser = new XPathParser();
    }

    /**
     * <UserInfoRequest>
     *   <header>
     *     <partnerId>STL</partnerId>
     *     <partnerTransactionId>87c9-d8c7-4e3f-aafd-8f4b8c2fafe1</partnerTransactionId>
     *     <partnerTimestamp>2020-03-04T13:05:21.3222285-08:00</partnerTimestamp>
     *     <application>STL</application>
     *     <channel>STL</channel>
     *     <dealerCode>0000002</dealerCode>
     *   </header>
     *   <userId>CAcuna6</userId>
     *   <systemIdsToRetrieve>Streamline</systemIdsToRetrieve>
     *   <returnAccountInfo>true</returnAccountInfo>
     * </UserInfoRequest>
     */
    @Multiline
    static String xml;

    @Test
    void noNamespaces() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, xml)
                .build();
        Message output = parser
                .expression("userId", "/UserInfoRequest/userId/text()")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("userId", "CAcuna6")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void noMatch() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, xml)
                .build();
        Message output = parser
                .expression("field", "//no/match/text()")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("field", "")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void invalidExpression() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> parser.expression("field", ",invalid/expr/text()"));
    }

    /**
     * <UserInfoRequest xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     *    <header xmlns="http://retail.tmobile.com/sdo">
     *       <partnerId>STL</partnerId>
     *       <partnerTransactionId>87c9-d8c7-4e3f-aafd-8f4b8c2fafe1</partnerTransactionId>
     *       <partnerTimestamp>2020-03-04T13:05:21.3222285-08:00</partnerTimestamp>
     *       <application>STL</application>
     *       <channel>STL</channel>
     *       <dealerCode>0000002</dealerCode>
     *    </header>
     *    <userId xmlns="http://retail.tmobile.com/sdo">CAcuna6</userId>
     *    <systemIdsToRetrieve xmlns="http://retail.tmobile.com/sdo">Streamline</systemIdsToRetrieve>
     *    <returnAccountInfo xmlns="http://retail.tmobile.com/sdo">true</returnAccountInfo>
     * </UserInfoRequest>
     */
    @Multiline
    static String privateNamespace;

    @Test
    void privateNamespace() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, privateNamespace)
                .build();
        Message output = parser
                .expression("channel", "//header[1]/channel/text()")
                .namespaceAware("false")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("channel", "STL")
                .build();
        assertThat(output, is(expected));
    }
}
