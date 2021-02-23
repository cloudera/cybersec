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
