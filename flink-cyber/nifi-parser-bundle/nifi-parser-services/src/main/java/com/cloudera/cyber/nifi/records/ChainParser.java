package com.cloudera.cyber.nifi.records;

import org.apache.nifi.serialization.record.Record;
import org.apache.nifi.serialization.record.RecordField;

import java.util.List;

public interface ChainParser {
    List<RecordField> getFields();

    Record nextRecord();
}
