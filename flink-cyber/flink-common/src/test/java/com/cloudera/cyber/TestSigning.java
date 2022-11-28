/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
