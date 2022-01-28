package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import com.cloudera.parserchains.queryservice.service.ChainPersistenceService;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.*;
import static com.cloudera.parserchains.queryservice.controller.ChainController.MAX_SAMPLES_PER_TEST;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ChainControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private ChainPersistenceService chainPersistenceService;
    private static int numFields = 0;
    private final String chainIdOne = "1";
    private final String chainNameOne = "chain1";

    @BeforeAll
    public static void beforeAll() {
        Method[] method = ParserChainSchema.class.getMethods();
        for (Method m : method) {
            if (m.getName().startsWith("set")) {
                numFields++;
            }
        }
    }

    @Test
    public void returns_list_of_all_chains() throws Exception {
        given(chainPersistenceService.findAll(isA(Path.class))).willReturn(
                Arrays.asList(
                        new ParserChainSummary().setId("1").setName("chain1"),
                        new ParserChainSummary().setId("2").setName("chain2"),
                        new ParserChainSummary().setId("3").setName("chain3")
                ));
        mvc.perform(MockMvcRequestBuilders.get(API_CHAINS_URL)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.*", instanceOf(List.class)))
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.[0].id", is("1")))
                .andExpect(jsonPath("$.[0].name", is("chain1")))
                .andExpect(jsonPath("$.[1].id", is("2")))
                .andExpect(jsonPath("$.[1].name", is("chain2")))
                .andExpect(jsonPath("$.[2].id", is("3")))
                .andExpect(jsonPath("$.[2].name", is("chain3")));
    }

    @Test
    public void returns_empty_list_when_no_chains() throws Exception {
        given(chainPersistenceService.findAll(isA(Path.class))).willReturn(Collections.emptyList());
        mvc.perform(MockMvcRequestBuilders.get(API_CHAINS_URL)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.*", instanceOf(List.class)))
                .andExpect(jsonPath("$.*", hasSize(0)));
    }

    /**
     * {
     *   "name" : "{name}"
     * }
     */
    @Multiline
    public static String createChainJSON;

    @Test
    public void creates_chain() throws Exception {
        String json = createChainJSON.replace("{name}", chainNameOne);
        ParserChainSchema chain = JSONUtils.INSTANCE.load(json, ParserChainSchema.class);
        ParserChainSchema expected = JSONUtils.INSTANCE.load(json, ParserChainSchema.class);
        expected.setId(chainIdOne);
        given(chainPersistenceService.create(eq(chain), isA(Path.class))).willReturn(expected);
        mvc.perform(MockMvcRequestBuilders.post(API_CHAINS_CREATE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(HttpHeaders.LOCATION,
                                API_CHAINS_READ_URL.replace("{id}", chainIdOne)))
                .andExpect(jsonPath("$.*", hasSize(numFields)))
                .andExpect(jsonPath("$.id", is(chainIdOne)))
                .andExpect(jsonPath("$.name", is(chainNameOne)));
    }

    /**
     * {
     *   "id" : "{id}",
     *   "name" : "{name}"
     * }
     */
    @Multiline
    public static String readChainJSON;

    @Test
    public void read_chain_by_id_returns_chain_config() throws Exception {
        String json = readChainJSON.replace("{id}", chainIdOne).replace("{name}", chainNameOne);
        final ParserChainSchema chain = JSONUtils.INSTANCE.load(json, ParserChainSchema.class);
        given(chainPersistenceService.read(eq(chainIdOne), isA(Path.class))).willReturn(chain);
        mvc.perform(
                MockMvcRequestBuilders
                        .get(API_CHAINS_READ_URL.replace("{id}", chainIdOne))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", is(chainIdOne)))
                .andExpect(jsonPath("$.name", is(chainNameOne)));
    }

    @Test
    public void read_chain_by_nonexistent_id_returns_not_found() throws Exception {
        given(chainPersistenceService.read(eq(chainIdOne), isA(Path.class))).willReturn(null);
        mvc.perform(
                MockMvcRequestBuilders
                        .get(API_CHAINS_READ_URL.replace("{id}", chainIdOne))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void update_chain_by_id_returns_updated_chain_config() throws Exception {
        String updateJson = readChainJSON.replace("{id}", chainIdOne).replace("{name}", chainNameOne);
        final ParserChainSchema updatedChain = JSONUtils.INSTANCE.load(updateJson, ParserChainSchema.class);
        given(chainPersistenceService.update(eq(chainIdOne), eq(updatedChain), isA(Path.class)))
                .willReturn(updatedChain);
        mvc.perform(MockMvcRequestBuilders
                .put(API_CHAINS_UPDATE_URL.replace("{id}", chainIdOne))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNoContent());
    }

    @Test
    public void update_chain_by_nonexistent_id_returns_not_found() throws Exception {
        given(chainPersistenceService.update(eq(chainIdOne), isA(ParserChainSchema.class), isA(Path.class)))
                .willReturn(null);
        mvc.perform(
                MockMvcRequestBuilders
                        .get(API_CHAINS_UPDATE_URL.replace("{id}", chainIdOne))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleting_existing_chain_succeeds() throws Exception {
        given(chainPersistenceService.delete(eq(chainIdOne), isA(Path.class))).willReturn(true);
        mvc.perform(
                MockMvcRequestBuilders
                        .delete(API_CHAINS_DELETE_URL.replace("{id}", chainIdOne))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleting_nonexistent_chain_returns_not_found() throws Exception {
        given(chainPersistenceService.delete(eq(chainIdOne), isA(Path.class))).willReturn(false);
        mvc.perform(
                MockMvcRequestBuilders
                        .delete(API_CHAINS_DELETE_URL.replace("{id}", chainIdOne))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * {
     *   "sampleData": {
     *     "type": "manual",
     *     "source": "Marie, Curie"
     *   },
     *   "chainConfig": {
     *     "id": "3b31e549-340f-47ce-8a71-d702685137f4",
     *     "name": "My Parser Chain",
     *     "parsers": [
     *       {
     *         "id": "61e99275-e076-46b6-aaed-8acce58cc0e4",
     *         "name": "Rename Field",
     *         "type": "com.cloudera.parserchains.parsers.RenameFieldParser",
     *         "config": {
     *           "fieldToRename": [
     *             {
     *               "from": "original_string",
     *               "to": "ORIGINAL_STRING"
     *             }
     *           ]
     *         }
     *       }, {
     *         "id": "1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *         "name": "Rename Field",
     *         "type": "com.cloudera.parserchains.parsers.RenameFieldParser",
     *         "config": {
     *           "fieldToRename": [
     *             {
     *               "from": "ORIGINAL_STRING",
     *               "to": "original_string"
     *             }
     *           ]
     *         }
     *       }
     *     ]
     *   }
     * }
     */
    @Multiline
    static String test_chain_request;

    /**
     * {
     *    "results":[
     *       {
     *          "input":{
     *             "original_string":"Marie, Curie"
     *          },
     *          "output":{
     *             "original_string":"Marie, Curie"
     *          },
     *          "log":{
     *             "type":"info",
     *             "message":"success",
     *             "parserId":"1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *             "parserName":"Rename Field"
     *          },
     *          "parserResults":[
     *             {
     *                "input":{
     *                   "original_string":"Marie, Curie"
     *                },
     *                "output":{
     *                   "ORIGINAL_STRING":"Marie, Curie"
     *                },
     *                "log":{
     *                   "type":"info",
     *                   "message":"success",
     *                   "parserId":"61e99275-e076-46b6-aaed-8acce58cc0e4",
     *                   "parserName":"Rename Field"
     *                }
     *             },
     *             {
     *                "input":{
     *                   "ORIGINAL_STRING":"Marie, Curie"
     *                },
     *                "output":{
     *                   "original_string":"Marie, Curie"
     *                },
     *                "log":{
     *                   "type":"info",
     *                   "message":"success",
     *                   "parserId":"1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *                   "parserName":"Rename Field"
     *                }
     *             }
     *          ]
     *       }
     *    ]
     * }
     */
    @Multiline
    static String test_chain_response;

    @Test
    void test_chain() throws Exception {
        RequestBuilder postRequest = MockMvcRequestBuilders
                .post(API_PARSER_TEST_URL)
                .content(test_chain_request)
                .contentType(MediaType.APPLICATION_JSON);
        String response = mvc.perform(postRequest)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ChainTestResponse actual = JSONUtils.INSTANCE.load(response, ChainTestResponse.class);
        ChainTestResponse expected = JSONUtils.INSTANCE.load(test_chain_response, ChainTestResponse.class);
        assertThat(actual, is(expected));
    }

    /**
     * {
     *   "sampleData": {
     *     "type": "manual",
     *     "source": [
     *          "Marie, Curie",
     *          "Ada, Lovelace"
     *      ]
     *   },
     *   "chainConfig": {
     *     "id": "3b31e549-340f-47ce-8a71-d702685137f4",
     *     "name": "My Parser Chain",
     *     "parsers": [
     *       {
     *         "id": "61e99275-e076-46b6-aaed-8acce58cc0e4",
     *         "name": "Rename Field",
     *         "type": "com.cloudera.parserchains.parsers.RenameFieldParser",
     *         "config": {
     *           "fieldToRename": [
     *             {
     *               "from": "original_string",
     *               "to": "ORIGINAL_STRING"
     *             }
     *           ]
     *         }
     *       }, {
     *         "id": "1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *         "name": "Rename Field",
     *         "type": "com.cloudera.parserchains.parsers.RenameFieldParser",
     *         "config": {
     *           "fieldToRename": [
     *             {
     *               "from": "ORIGINAL_STRING",
     *               "to": "original_string"
     *             }
     *           ]
     *         }
     *       }
     *     ]
     *   }
     * }
     */
    @Multiline
    static String test_chain_with_2_samples_request;

    /**
     * {
     *    "results":[
     *       {
     *          "input":{
     *             "original_string":"Marie, Curie"
     *          },
     *          "output":{
     *             "original_string":"Marie, Curie"
     *          },
     *          "log":{
     *             "type":"info",
     *             "message":"success",
     *             "parserId":"1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *             "parserName":"Rename Field"
     *          },
     *          "parserResults":[
     *             {
     *                "input":{
     *                   "original_string":"Marie, Curie"
     *                },
     *                "output":{
     *                   "ORIGINAL_STRING":"Marie, Curie"
     *                },
     *                "log":{
     *                   "type":"info",
     *                   "message":"success",
     *                   "parserId":"61e99275-e076-46b6-aaed-8acce58cc0e4",
     *                   "parserName":"Rename Field"
     *                }
     *             },
     *             {
     *                "input":{
     *                   "ORIGINAL_STRING":"Marie, Curie"
     *                },
     *                "output":{
     *                   "original_string":"Marie, Curie"
     *                },
     *                "log":{
     *                   "type":"info",
     *                   "message":"success",
     *                   "parserId":"1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *                   "parserName":"Rename Field"
     *                }
     *             }
     *          ]
     *       },
     *       {
     *          "input":{
     *             "original_string":"Ada, Lovelace"
     *          },
     *          "output":{
     *             "original_string":"Ada, Lovelace"
     *          },
     *          "log":{
     *             "type":"info",
     *             "message":"success",
     *             "parserId":"1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *             "parserName":"Rename Field"
     *          },
     *          "parserResults":[
     *             {
     *                "input":{
     *                   "original_string":"Ada, Lovelace"
     *                },
     *                "output":{
     *                   "ORIGINAL_STRING":"Ada, Lovelace"
     *                },
     *                "log":{
     *                   "type":"info",
     *                   "message":"success",
     *                   "parserId":"61e99275-e076-46b6-aaed-8acce58cc0e4",
     *                   "parserName":"Rename Field"
     *                }
     *             },
     *             {
     *                "input":{
     *                   "ORIGINAL_STRING":"Ada, Lovelace"
     *                },
     *                "output":{
     *                   "original_string":"Ada, Lovelace"
     *                },
     *                "log":{
     *                   "type":"info",
     *                   "message":"success",
     *                   "parserId":"1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *                   "parserName":"Rename Field"
     *                }
     *             }
     *          ]
     *       }
     *    ]
     * }
     */
    @Multiline
    static String test_chain_with_2_samples_response;

    @Test
    void test_chain_with_2_samples() throws Exception {
        RequestBuilder postRequest = MockMvcRequestBuilders
                .post(API_PARSER_TEST_URL)
                .content(test_chain_with_2_samples_request)
                .contentType(MediaType.APPLICATION_JSON);
        String response = mvc.perform(postRequest)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ChainTestResponse actual = JSONUtils.INSTANCE.load(response, ChainTestResponse.class);
        ChainTestResponse expected = JSONUtils.INSTANCE.load(test_chain_with_2_samples_response, ChainTestResponse.class);
        assertThat(actual, is(expected));
    }

    /**
     * {
     *   "sampleData": {
     *     "type": "manual",
     *     "source": [
     *          "Marie, Curie",
     *          "Ada, Lovelace"
     *      ]
     *   },
     *   "chainConfig": {
     *     "id": "3b31e549-340f-47ce-8a71-d702685137f4",
     *     "name": "Chain with Invalid Router",
     *     "parsers": [
     *       {
     *         "id": "61e99275-e076-46b6-aaed-8acce58cc0e4",
     *         "name": "Invalid Router",
     *         "type": "Router",
     *         "config": {
     *         },
     *         "routing":{
     *           "routes":[
     *            ]
     *         }
     *       }
     *     ]
     *   }
     * }
     */
    @Multiline
    static String test_invalid_chain_request;

    /**
     * {
     *    "results":[
     *       {
     *          "input":{
     *          },
     *          "output":{
     *          },
     *          "log":{
     *             "type":"error",
     *             "message":"Invalid field name: 'null'",
     *             "parserId":"61e99275-e076-46b6-aaed-8acce58cc0e4"
     *          }
     *       },
     *       {
     *          "input":{
     *          },
     *          "output":{
     *          },
     *          "log":{
     *             "type":"error",
     *             "message":"Invalid field name: 'null'",
     *             "parserId":"61e99275-e076-46b6-aaed-8acce58cc0e4"
     *          }
     *       }
     *    ]
     * }
     */
    @Multiline
    static String test_invalid_chain_response;

    @Test
    void test_invalid_chain() throws Exception {
        RequestBuilder postRequest = MockMvcRequestBuilders
                .post(API_PARSER_TEST_URL)
                .content(test_invalid_chain_request)
                .contentType(MediaType.APPLICATION_JSON);
        String response = mvc.perform(postRequest)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ChainTestResponse actual = JSONUtils.INSTANCE.load(response, ChainTestResponse.class);
        ChainTestResponse expected = JSONUtils.INSTANCE.load(test_invalid_chain_response, ChainTestResponse.class);
        assertThat(actual, is(expected));
    }

    @Test
    void test_chain_with_too_many_samples() throws Exception {
        // add to the request more test samples than are allowed
        String sample = "a sample of text to parse";
        ChainTestRequest testRequest = JSONUtils.INSTANCE.load(test_chain_request, ChainTestRequest.class);
        IntStream.range(0, MAX_SAMPLES_PER_TEST + 10).forEach(i -> testRequest.getSampleData().addSource(sample));

        // expect the number of results returned to be capped at the maximum allowed
        String request = JSONUtils.INSTANCE.toJSON(testRequest, true);
        RequestBuilder postRequest = MockMvcRequestBuilders
                .post(API_PARSER_TEST_URL)
                .content(request)
                .contentType(MediaType.APPLICATION_JSON);
        mvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", instanceOf(List.class)))
                .andExpect(jsonPath("$.results", hasSize(MAX_SAMPLES_PER_TEST)));
    }

    /**
     * {
     *    "sampleData":{
     *       "type":"manual",
     *       "source":[
     *          "ASas"
     *       ]
     *    },
     *    "chainConfig":{
     *       "id":"1",
     *       "name":"hello",
     *       "parsers":[
     *          {
     *             "name":"Syslog",
     *             "type":"com.cloudera.parserchains.parsers.SyslogParser",
     *             "id":"8f498980-5f13-11ea-9ea2-a3a38413c812",
     *             "config":{
     *             }
     *          }
     *       ]
     *    }
     * }
     */
    @Multiline
    static String test_get_useful_error_message;

    @Test
    void test_get_useful_error_message() throws Exception {
        /*
         * in some cases the root exception doesn't contain
         * a useful error message.  this test ensures that
         * we always get a useful error message for the user.
         */
        RequestBuilder postRequest = MockMvcRequestBuilders
                .post(API_PARSER_TEST_URL)
                .content(test_get_useful_error_message)
                .contentType(MediaType.APPLICATION_JSON);
        mvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", instanceOf(List.class)))
                .andExpect(jsonPath("$.results", hasSize(1)))

                // the error result for the first message
                .andExpect(jsonPath("$.results.[0].log.type", is("error")))
                .andExpect(jsonPath("$.results.[0].log.parserId", is("8f498980-5f13-11ea-9ea2-a3a38413c812")))
                .andExpect(jsonPath("$.results.[0].log.message",
                        is("Syntax error @ 1:0 mismatched input 'A' expecting {' ', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '<'}")));
    }
}
