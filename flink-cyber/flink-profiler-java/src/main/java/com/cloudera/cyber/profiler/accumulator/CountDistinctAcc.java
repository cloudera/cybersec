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

import org.apache.flink.api.common.accumulators.Accumulator;

public class CountDistinctAcc implements Accumulator<String, SerializableUnion> {
    private final SerializableUnion unionWrapper = new SerializableUnion();

    @Override
    public void add(String stringValue) {
        if (stringValue != null) {
            unionWrapper.getUnion().update(stringValue);
        }
    }

    @Override
    public SerializableUnion getLocalValue() {
        return unionWrapper;
    }

    @Override
    public void resetLocal() {
        unionWrapper.getUnion().reset();
    }

    @Override
    public void merge(Accumulator<String, SerializableUnion> other) {
        if (other != null) {
            unionWrapper.getUnion().update(other.getLocalValue().getUnion().getResult());
        }
    }


    @Override
    public Accumulator<String, SerializableUnion> clone() {
        CountDistinctAcc result = new CountDistinctAcc();
        result.unionWrapper.getUnion().update(unionWrapper.getUnion().getResult());
        return result;
    }

    public double getEstimate() {
        return unionWrapper.getUnion().getResult().getEstimate();
    }
}
