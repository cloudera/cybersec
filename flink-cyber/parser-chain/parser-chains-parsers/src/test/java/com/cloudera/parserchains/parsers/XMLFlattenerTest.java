package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.Message;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class XMLFlattenerTest {
    private XMLFlattener parser;

    @BeforeEach
    void beforeEach() {
        parser = new XMLFlattener();
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
    static String flatten;

    @Test
    void flatten() {
        Message input = Message.builder()
                .addField("input", flatten)
                .build();
        Message output = parser
                .inputField("input")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("UserInfoRequest.header.partnerId", "STL")
                .addField("UserInfoRequest.header.partnerTransactionId", "87c9-d8c7-4e3f-aafd-8f4b8c2fafe1")
                .addField("UserInfoRequest.header.partnerTimestamp", "2020-03-04T13:05:21.3222285-08:00")
                .addField("UserInfoRequest.header.application", "STL")
                .addField("UserInfoRequest.header.channel", "STL")
                .addField("UserInfoRequest.header.dealerCode", "0000002")
                .addField("UserInfoRequest.userId", "CAcuna6")
                .addField("UserInfoRequest.systemIdsToRetrieve", "Streamline")
                .addField("UserInfoRequest.returnAccountInfo", "true")
                .build();
        assertThat(output, is(expected));
    }

    /**
     * <breakfast>
     *    <food>
     *       <name>Belgian Waffles</name>
     *       <price>$5.95</price>
     *       <calories>650</calories>
     *    </food>
     *    <food>
     *       <name>French Toast</name>
     *       <price>$4.50</price>
     *       <calories>600</calories>
     *    </food>
     * </breakfast>
     */
    @Multiline
    static String arrays;

    @Test
    void flattenArrays() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, arrays)
                .build();
        Message output = parser.parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("breakfast.food[0].name", "Belgian Waffles")
                .addField("breakfast.food[0].price", "$5.95")
                .addField("breakfast.food[0].calories", "650")
                .addField("breakfast.food[1].name", "French Toast")
                .addField("breakfast.food[1].price", "$4.50")
                .addField("breakfast.food[1].calories", "600")
                .build();
        assertThat(output, is(expected));
    }

    /**
     * <breakfast>
     *    <food>
     *       <name>Belgian Waffles</name>
     *       <price type="USD">$5.95</price>
     *       <calories type="kcals">650</calories>
     *    </food>
     * </breakfast>
     */
    @Multiline
    static String flattenAttributes;

    @Test
    void flattenAttributes() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, flattenAttributes)
                .build();
        Message output = parser.parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("breakfast.food.name", "Belgian Waffles")
                .addField("breakfast.food.price", "$5.95")
                .addField("breakfast.food.price.type", "USD")
                .addField("breakfast.food.calories", "650")
                .addField("breakfast.food.calories.type", "kcals")
                .build();
        assertThat(output, is(expected));
    }

    /**
     * <breakfast>
     *    <food>
     *       <name>Belgian Waffles</name>
     *       <price type="USD">5.95</price>
     *       <calories type="kcals">650</calories>
     *    </food>
     * </breakfast>
     */
    @Multiline
    static String setSeparator;

    @Test
    void setSeparator() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, setSeparator)
                .build();
        Message output = parser
                .separator("-")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("breakfast-food-name", "Belgian Waffles")
                .addField("breakfast-food-price", "5.95")
                .addField("breakfast-food-price-type", "USD")
                .addField("breakfast-food-calories", "650")
                .addField("breakfast-food-calories-type", "kcals")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void missingInputField() {
        Message input = Message.builder()
                .addField("foo", "bar")
                .build();
        Message output = parser
                .inputField("input")
                .parse(input);
        assertThat("Expected all the input fields to remain.",
                output.getFields(), is(input.getFields()));
        assertThat("Expected an error to have occurred.",
                output.getError().isPresent(), is(true));
    }

    @Test
    void invalidXML() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, "<breakfast ><food>")
                .build();
        Message output = parser.parse(input);
        assertThat("Expected all the input fields to remain.",
                output.getFields(), is(input.getFields()));
        assertThat("Expected an error to have occurred.",
                output.getError().isPresent(), is(true));
    }

    @Test
    void emptyXml() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, "<breakfast/>")
                .build();
        Message output = parser.parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("breakfast", "")
                .build();
        assertThat(output, is(expected));
    }
}
