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

import org.apache.datasketches.memory.Memory;
import org.apache.datasketches.theta.SetOperation;
import org.apache.datasketches.theta.Sketch;
import org.apache.datasketches.theta.Sketches;
import org.apache.datasketches.theta.Union;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.apache.datasketches.theta.SetOperation.builder;

public class SerializableUnion implements Serializable {

    private transient Union union;

    public SerializableUnion() {
        union = SetOperation.builder().buildUnion();
    }

    Union getUnion() {
        return union;
    }

    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        oos.defaultWriteObject();
        byte[] unionBytes = union.getResult().toByteArray();
        oos.writeInt(unionBytes.length);
        oos.write(unionBytes);
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        int unionByteLength = ois.readInt();
        byte[] unionBytes = new byte[unionByteLength];
        ois.readFully(unionBytes);
        Sketch sketch = Sketches.wrapSketch(Memory.wrap(unionBytes));
        union = builder().buildUnion();
        union.update(sketch);
    }
}
