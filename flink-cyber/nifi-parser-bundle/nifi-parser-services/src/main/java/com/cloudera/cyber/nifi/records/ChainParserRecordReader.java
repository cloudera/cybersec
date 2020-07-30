package com.cloudera.cyber.nifi.records;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.serialization.MalformedRecordException;
import org.apache.nifi.serialization.RecordReader;
import org.apache.nifi.serialization.SimpleRecordSchema;
import org.apache.nifi.serialization.record.Record;
import org.apache.nifi.serialization.record.RecordSchema;

import java.io.IOException;

/**
 * RecordReader to apply a Cloudera Cyber chain parser to the records and produce Message compatible objects
 */
@RequiredArgsConstructor
public class ChainParserRecordReader implements RecordReader {
    @NonNull private ChainParser parser;
    @NonNull private ComponentLog logger;

    @Override
    public Record nextRecord() throws IOException, MalformedRecordException {
        return nextRecord(true, false);
    }

    @Override
    public Record nextRecord(boolean coerceTypes, boolean dropUnknownFields) throws IOException, MalformedRecordException {
        return parser.nextRecord();
    }

    @Override
    public RecordSchema getSchema() throws MalformedRecordException {
        return new SimpleRecordSchema(this.parser.getFields());
    }

    @Override
    public void close() throws IOException {

    }
}
