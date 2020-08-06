package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DelimitedKeyValueParserTest {
    private DelimitedKeyValueParser parser;

    @BeforeEach
    void beforeEach() {
        parser = new DelimitedKeyValueParser();
    }

    @Test
    void success() {
        final String textToParse = "HostName=PRDPWSLNX0067||Operation=getUserInfo||Is_Operation_Success=SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Operation", "getUserInfo")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void setDelimiter() {
        final String textToParse = "HostName=PRDPWSLNX0067,Operation=getUserInfo,Is_Operation_Success=SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .keyValueDelimiter(",")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Operation", "getUserInfo")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void setSeparator() {
        final String textToParse = "HostName->PRDPWSLNX0067||Operation->getUserInfo||Is_Operation_Success->SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .keyValueSeparator("->")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Operation", "getUserInfo")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void setInputField() {
        final String textToParse = "HostName=PRDPWSLNX0067||Operation=getUserInfo||Is_Operation_Success=SUCCESS";
        Message input = Message.builder()
                .addField("input", textToParse)
                .build();
        Message output = parser
                .inputField("input")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Operation", "getUserInfo")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void leadingDelimiter() {
        final String textToParse = "||HostName=PRDPWSLNX0067||Operation=getUserInfo||";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Operation", "getUserInfo")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void trailingDelimiter() {
        final String textToParse = "HostName=PRDPWSLNX0067||";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void emptyValue() {
        final String textToParse = "HostName=PRDPWSLNX0067||Is_Operation_Success=";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Is_Operation_Success","")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void ignoreMissing() {
        final String textToParse = "HostName=PRDPWSLNX0067||||Is_Operation_Success=SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void ignoreBlank() {
        final String textToParse = "HostName=PRDPWSLNX0067||  ||Is_Operation_Success=SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void ignoreMissingWithExtraDelimiter() {
        final String textToParse = "HostName=PRDPWSLNX0067||||Is_Operation_Success=SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void ignoreMissingKey() {
        final String textToParse = "HostName=PRDPWSLNX0067||=SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void ignoreInvalidKey() {
        final String textToParse = "HostName=PRDPWSLNX0067||AuditMessage; Interaction_Id=value||Is_Operation_Success=SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .validKeyRegex("[a-zA-Z_]+")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("HostName", "PRDPWSLNX0067")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void ignoreInvalidValue() {
        final String textToParse = "HostName=PRDPWSLNX0067||AuditMessage; Interaction_Id=value||Is_Operation_Success=SUCCESS";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .validValueRegex("[a-zA-Z_]+")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("AuditMessage; Interaction_Id", "value")
                .addField("Is_Operation_Success","SUCCESS")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void unsetValidKeyRegex() {
        assertThat("Defining valid key regex as empty string should ensure no regex matching occurs.",
                parser.validKeyRegex("").getValidKeyRegex().isPresent(), is(false));
        assertThat("Defining valid key regex as blank should ensure no regex matching occurs.",
                parser.validKeyRegex("    ").getValidKeyRegex().isPresent(), is(false));
    }

    @Test
    void unsetValidValueRegex() {
        assertThat("Defining valid value regex as empty string should ensure no regex matching occurs.",
                parser.validValueRegex("").getValidValueRegex().isPresent(), is(false));
        assertThat("Defining valid value regex as blank should ensure no regex matching occurs.",
                parser.validValueRegex("    ").getValidValueRegex().isPresent(), is(false));
    }

    @Test
    void complex() {
        final String textToParse = "2020-03-04 13:05:21.322- AuditOperation - HostName=PRDPWSLNX0067" +
                "||Operation=getUserInfo" +
                "||Is_Operation_Success=SUCCESS" +
                "||AuditMessage; Interaction_Id=" +
                "||PayType=DEFAULT" +
                "||Operation_Type=Tibco_Service_Call" +
                "||Service_Name=getUserInfo" +
                "||Message=Invoking:#UserInfoService.getUserInfo tibco call with request:#Invoke of Streamline.Tibco.Services.GetUserInfoService.UserInfoService.getUserInfo started at 03/04/2020 13:05:21.322 with parameters: parameter 0 (serialized .NET Object): <?xml version=\"1.0\" encoding=\"utf-16\"?> <UserInfoRequest xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"> <header xmlns=\"http://retail.tmobile.com/sdo\"> <partnerId>STL</partnerId> <partnerTransactionId>87c9-d8c7-4e3f-aafd-8f4b8c2fafe1</partnerTransactionId> <partnerTimestamp>2020-03-04T13:05:21.3222285-08:00</partnerTimestamp> <application>STL</application> <channel>STL</channel> <dealerCode>0000002</dealerCode> </header> <userId xmlns=\"http://retail.tmobile.com/sdo\">CAcuna6</userId> <systemIdsToRetrieve xmlns=\"http://retail.tmobile.com/sdo\">Streamline</systemIdsToRetrieve> <returnAccountInfo xmlns=\"http://retail.tmobile.com/sdo\">true</returnAccountInfo> </UserInfoRequest> URL = https://rspservices.t-mobile.com/rsp/UserInfoService Timeout = 10000 " +
                "||LogType=Call" +
                "||Method_Name=Invoke" +
                "||Reason=";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = parser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("2020-03-04 13:05:21.322- AuditOperation - HostName", "PRDPWSLNX0067")
                .addField("Operation", "getUserInfo")
                .addField("Is_Operation_Success","SUCCESS")
                .addField("AuditMessage; Interaction_Id", "")
                .addField("PayType", "DEFAULT")
                .addField("Operation_Type", "Tibco_Service_Call")
                .addField("Service_Name", "getUserInfo")
                .addField("Message", "Invoking:#UserInfoService.getUserInfo tibco call with request:#Invoke of Streamline.Tibco.Services.GetUserInfoService.UserInfoService.getUserInfo started at 03/04/2020 13:05:21.322 with parameters: parameter 0 (serialized .NET Object): <?xml version=\"1.0\" encoding=\"utf-16\"?> <UserInfoRequest xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"> <header xmlns=\"http://retail.tmobile.com/sdo\"> <partnerId>STL</partnerId> <partnerTransactionId>87c9-d8c7-4e3f-aafd-8f4b8c2fafe1</partnerTransactionId> <partnerTimestamp>2020-03-04T13:05:21.3222285-08:00</partnerTimestamp> <application>STL</application> <channel>STL</channel> <dealerCode>0000002</dealerCode> </header> <userId xmlns=\"http://retail.tmobile.com/sdo\">CAcuna6</userId> <systemIdsToRetrieve xmlns=\"http://retail.tmobile.com/sdo\">Streamline</systemIdsToRetrieve> <returnAccountInfo xmlns=\"http://retail.tmobile.com/sdo\">true</returnAccountInfo> </UserInfoRequest> URL = https://rspservices.t-mobile.com/rsp/UserInfoService Timeout = 10000 ")
                .addField("LogType", "Call")
                .addField("Method_Name", "Invoke")
                .addField("Reason", "")
                .build();
        assertThat(output, is(expected));
    }



}
