package com.cloudera.cyber.nifi.records;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.schema.access.SchemaNotFoundException;
import org.apache.nifi.serialization.MalformedRecordException;
import org.apache.nifi.serialization.RecordReader;
import org.apache.nifi.serialization.RecordReaderFactory;

import java.io.IOException;
import java.io.InputStream;

@Tags({ "parse", "cyber", "record"})
@CapabilityDescription("Reads raw data and parses using Cloudera Cyber Chain Parsers")
public abstract class ChainParserReader extends AbstractControllerService implements RecordReaderFactory {
    public RecordReader createRecordReader(FlowFile flowFile, InputStream in, ComponentLog logger)
            throws MalformedRecordException, IOException, SchemaNotFoundException {

        ChainParser parser = new DummyParser();
        return new ChainParserRecordReader(parser,logger);
    }

}
