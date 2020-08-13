package com.cloudera.cyber;

import com.cloudera.cyber.flink.Utils;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.collection.IsArrayWithSize;
import org.junit.Test;

import java.security.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestSigning {
    
    @Test
    public void testSign() throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        gen.initialize(1024, new SecureRandom());
        KeyPair pair = gen.generateKeyPair();
        KeyPair pair2 = gen.generateKeyPair();

        String data = "fsdfdsfgsfaggsrt3f4e42";

        PrivateKey key;
        byte[] out = Utils.sign(data, pair.getPrivate());
        assertThat("Bytes are signed", ArrayUtils.toObject(out), IsArrayWithSize.arrayWithSize(128));

        assertThat("verifies correctly", Utils.verify(data, out, pair.getPublic()), is(true));
        assertThat("verifies messed about data fails", Utils.verify("fsdfsaf", out, pair.getPublic()), is(false));
        assertThat("verifies wrong key fails", Utils.verify(data, out, pair2.getPublic()), is(false));
    }
    
}
