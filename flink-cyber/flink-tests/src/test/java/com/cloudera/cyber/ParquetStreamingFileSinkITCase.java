package com.cloudera.cyber;

import com.cloudera.cyber.parser.MessageToParse;
import org.apache.avro.generic.GenericData;
import org.apache.avro.specific.SpecificData;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.util.FiniteTestSource;
import org.apache.flink.test.util.AbstractTestBase;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Simple integration test case for writing bulk encoded files with the
 * {@link StreamingFileSink} with Parquet.
 */
@SuppressWarnings("serial")
public class ParquetStreamingFileSinkITCase extends AbstractTestBase {

    @Rule
    public final Timeout timeoutPerTest = Timeout.seconds(20);

    @Test
    public void testWriteParquetAvroSpecific() throws Exception {

        final File folder = TEMPORARY_FOLDER.newFolder();

        final List<MessageToParse> data = Arrays.asList(MessageToParse.newBuilder()
                        .setOffset(0)
                        .setPartition(0)
                        .setTopic("test")
                        .setOriginalSource("original")
                        .build(),
                MessageToParse.newBuilder()
                        .setOffset(1)
                        .setPartition(0)
                        .setTopic("test2")
                        .setOriginalSource("original2")
                        .build()
        );
        
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(100);

        DataStream<MessageToParse> stream = env.addSource(
                new FiniteTestSource<>(data), TypeInformation.of(MessageToParse.class));

        stream.addSink(
                StreamingFileSink.forBulkFormat(
                        Path.fromLocalFile(folder),
                        ParquetAvroWriters.forSpecificRecord(MessageToParse.class))
                        .build());

        env.execute();

        validateResults(folder, SpecificData.get(), data);
    }


    private static <T> void validateResults(File folder, GenericData dataModel, List<T> expected) throws Exception {
        File[] buckets = folder.listFiles();
        assertNotNull(buckets);
        assertEquals(1, buckets.length);

        File[] partFiles = buckets[0].listFiles();
        assertNotNull(partFiles);
        assertEquals(2, partFiles.length);

        for (File partFile : partFiles) {
            assertTrue(partFile.length() > 0);

            final List<Tuple2<Long, String>> fileContent = readParquetFile(partFile, dataModel);
            assertEquals(expected, fileContent);
        }
    }

    private static <T> List<T> readParquetFile(File file, GenericData dataModel) throws IOException {
        InputFile inFile = HadoopInputFile.fromPath(new org.apache.hadoop.fs.Path(file.toURI()), new Configuration());

        ArrayList<T> results = new ArrayList<>();
        try (ParquetReader<T> reader = AvroParquetReader.<T>builder(inFile).withDataModel(dataModel).build()) {
            T next;
            while ((next = reader.read()) != null) {
                results.add(next);
            }
        }

        return results;
    }


}