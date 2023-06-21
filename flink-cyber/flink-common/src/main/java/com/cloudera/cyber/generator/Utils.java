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

package com.cloudera.cyber.generator;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.flink.core.fs.Path;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


public class Utils {

   public static GenericRecord jsonDecodeToAvroGenericRecord(String json, Schema schema) {
      try {
         JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, json);
         DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);
         return datumReader.read(null, jsonDecoder);
      } catch (IOException e) {
         return null;
      }
   }

   public static byte[] jsonDecodeToAvroByteArray(String json, Schema schema) {
      GenericRecord record = jsonDecodeToAvroGenericRecord(json, schema);
      DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
         BinaryEncoder binaryEncoder = EncoderFactory.get().directBinaryEncoder(out, null);
         datumWriter.write(record, binaryEncoder);
         return out.toByteArray();
      } catch (IOException exception) {
         return null;
      }
   }

    public static InputStream openFileStream(String baseDir, String filePath) throws IOException {
        Path possiblePath = new Path(filePath);
        Path resolvedPath = null;
        if (possiblePath.getFileSystem().exists(possiblePath)) {
            resolvedPath = possiblePath;
        } else {
            if (baseDir != null && !baseDir.isEmpty() && !possiblePath.isAbsolute()) {
                possiblePath = new Path(baseDir, filePath);
                if (possiblePath.getFileSystem().exists(possiblePath)) {
                    resolvedPath = possiblePath;
                }
            }
        }
        if (resolvedPath != null) {
            return resolvedPath.getFileSystem().open(resolvedPath);
        } else {
            throw new FileNotFoundException(String.format("Basedir: '%s' File: '%s'", baseDir, filePath));
        }
    }
}
