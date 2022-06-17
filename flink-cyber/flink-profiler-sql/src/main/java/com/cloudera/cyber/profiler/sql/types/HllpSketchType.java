package com.cloudera.cyber.profiler.sql.types;

import org.apache.datasketches.hll.HllSketch;
import org.apache.flink.table.annotation.DataTypeHint;

public class HllpSketchType {
    public @DataTypeHint("RAW")
    HllSketch sketch;
}
