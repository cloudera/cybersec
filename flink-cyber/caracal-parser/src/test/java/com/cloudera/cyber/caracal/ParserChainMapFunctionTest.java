package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.parserchains.core.utils.JSONUtils;
import lombok.NonNull;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.parser.ParserJob.PARAM_PRIVATE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasKey;

public class ParserChainMapFunctionTest {
    /**
     * [
     *   {
     *     "topic": "test",
     *     "splitPath": "$.http-stream['http.request'][*]",
     *     "headerPath": "$.http-stream",
     *     "timestampField" : "start_ts",
     *     "timestampFunction": "Math.round(parseFloat(ts)*1000,0)",
     *     "chainSchema": {
     *       "id": "3b31e549-340f-47ce-8a71-d702685137f4",
     *       "name": "My Parser Chain",
     *       "parsers": [
     *         {
     *           "id": "1ee889fc-7495-4b47-8243-c16e5e74bb82",
     *           "name": "Rename Field",
     *           "type": "com.cloudera.parserchains.parsers.RenameFieldParser",
     *           "config": {
     *             "fieldToRename": [
     *               {
     *                 "from": "ip_src",
     *                 "to": "ip_src_addr"
     *               },
     *               {
     *                 "from": "ip_dst",
     *                 "to": "ip_dest_addr"
     *               }
     *             ]
     *           }
     *         }
     *       ]
     *     }
     *   }
     * ]
     */
    @Multiline
    private String splitConfigString = null;

    @Test
    public void testParserChain() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        gen.initialize(1024, new SecureRandom());
        KeyPair pair = gen.generateKeyPair();

        ParameterTool params = ParameterTool.fromMap(new HashMap<String,String>() {{
            put(PARAM_PRIVATE_KEY, Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        }});

        @NonNull List<SplitConfig> splitConfig = JSONUtils.INSTANCE.getMapper().readValue(splitConfigString, new com.fasterxml.jackson.core.type.TypeReference<List<SplitConfig>>() {});
        Map<String, SplitConfig> configMap = splitConfig.stream().collect(Collectors.toMap(k -> k.getTopic(), v->v));

        ParserChainMapFunction parserChainMapFunction = new ParserChainMapFunction(configMap);
        parserChainMapFunction.open(params.getConfiguration());
        Message output = parserChainMapFunction.map(createInput());
        assertThat("ip_src renamed", output.getExtensions(), allOf(hasKey("ip_src_addr"), not(hasKey("ip_src"))));
    }

    private Message createInput() {
        return Message.builder()
                .ts(Instant.now().toEpochMilli())
                .source("test")
                .originalSource(SignedSourceKey.builder()
                        .topic("topic")
                        .partition(0)
                        .offset(0)
                        .signature(new byte[128])
                        .build())
                .extensions(createFields()).build();
    }

    private Map<String, String> createFields() {
        return new HashMap<String, String>(){{
            put("test", "value");
            put("ip_src", "192.168.0.1");
            put("ip_dst", "8.8.8.8");
        }};
    }
}
