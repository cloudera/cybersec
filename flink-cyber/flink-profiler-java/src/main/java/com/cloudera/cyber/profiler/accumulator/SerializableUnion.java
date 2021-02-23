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
