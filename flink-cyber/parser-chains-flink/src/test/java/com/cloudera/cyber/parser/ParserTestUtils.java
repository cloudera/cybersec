package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.parser.wrappers.AbstractParserOutput;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.google.common.io.Resources;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ParserTestUtils {

    public static String readConfigFile(String name) throws IOException {
        URL url = Resources.getResource(name);
        return Resources.toString(url, UTF_8);
    }

    public static Signature loadSignature(PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException {
        Signature signature = null;
        if (privateKey != null) {
            signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(privateKey);
        }

        return signature;
    }

    public static PrivateKey loadPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        URL url = Resources.getResource("private_key.der");
        byte[] privKeyBytes = Resources.toByteArray(url);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
        return keyFactory.generatePrivate(privSpec);
    }

    public static ParserChainMap readParserChainMap(String chainConfigFile) throws IOException {
        String chainConfig = readConfigFile(chainConfigFile);
        return JSONUtils.INSTANCE.load(chainConfig, ParserChainMap.class);
    }

    public static TopicPatternToChainMap readTopicMap(String topicMapFile) throws IOException {
        String topicConfig = readConfigFile(topicMapFile);
         return JSONUtils.INSTANCE.load(topicConfig, TopicPatternToChainMap.class);
    }

    public static SignedSourceKey createExpectedOriginalSource(MessageToParse messageToParse, byte[] signature) {
        return SignedSourceKey.builder().
                topic(messageToParse.getTopic()).partition(messageToParse.getPartition()).
                offset(messageToParse.getOffset()).signature(signature).
                build();
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class TestParserOutput extends AbstractParserOutput {
        List<Message> output = new ArrayList<>();

        @Override
        public void output(Message message) {
            output.add(message);
        }
    }
}
