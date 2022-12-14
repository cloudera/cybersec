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

package com.cloudera.cyber.profiler.accumulator;

import com.google.common.collect.Lists;
import org.apache.datasketches.theta.Union;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.List;

public class SerializableUnionTest {

    @Test
    public void serializeUnion() throws IOException, ClassNotFoundException {
        SerializableUnion unionWrapper = new SerializableUnion();

        List<String> distinctStrings = Lists.newArrayList("String 1", "String 2", "String 3");
        Union union = unionWrapper.getUnion();
        // add strings and verify the distinct count
        distinctStrings.forEach(union::update);
        Assert.assertEquals(distinctStrings.size(), union.getResult().getEstimate(), 0.1);

        // add same strings again - distinct count should remain the same
        distinctStrings.forEach(union::update);
        Assert.assertEquals(3.0, union.getResult().getEstimate(), 0.1);

        // test serialization and deserialization
        testSerDe(unionWrapper);
    }

    private void testSerDe(SerializableUnion unionWrapper) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOutputStream
                = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream
                = new ObjectOutputStream(byteOutputStream);
        objectOutputStream.writeObject(unionWrapper);
        objectOutputStream.flush();
        objectOutputStream.close();

        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
        SerializableUnion deserUnionWrapper = (SerializableUnion) objectInputStream.readObject();
        Assert.assertEquals(unionWrapper.getUnion().getResult().getEstimate(), deserUnionWrapper.getUnion().getResult().getEstimate(), 0.1);
    }
}
