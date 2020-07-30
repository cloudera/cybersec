package com.cloudera.cyber.nifi.records;

import org.apache.nifi.serialization.record.Record;
import org.apache.nifi.serialization.record.RecordField;

import java.util.List;

public class DummyParser implements ChainParser {
    @Override
    public List<RecordField> getFields() {
        return null;
    }

    @Override
    public Record nextRecord() {
        return null;
    }
}
