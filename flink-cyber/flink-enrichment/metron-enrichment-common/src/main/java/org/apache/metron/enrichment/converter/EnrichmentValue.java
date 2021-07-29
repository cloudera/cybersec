/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.enrichment.converter;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.enrichment.lookup.LookupValue;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.Map;

public class EnrichmentValue implements LookupValue {
   private static final ThreadLocal<ObjectMapper> _mapper = new ThreadLocal<ObjectMapper>() {
             @Override
             protected ObjectMapper initialValue() {
                return new ObjectMapper();
             }
    };
    public static final String VALUE_COLUMN_NAME = "v";
    public static final byte[] VALUE_COLUMN_NAME_B = Bytes.toBytes(VALUE_COLUMN_NAME);

    private Map<String, Object> metadata = null;

    public EnrichmentValue()
    {

    }

    public EnrichmentValue(Map<String, Object> metadata) {
        this.metadata = metadata;
    }



    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public Iterable<Map.Entry<byte[], byte[]>> toColumns() {
        return AbstractConverter.toEntries( VALUE_COLUMN_NAME_B, Bytes.toBytes(valueToString(metadata))
                                  );
    }

    @Override
    public void fromColumns(Iterable<Map.Entry<byte[], byte[]>> values) {
        for(Map.Entry<byte[], byte[]> cell : values) {
            if(Bytes.equals(cell.getKey(), VALUE_COLUMN_NAME_B)) {
                metadata = stringToValue(Bytes.toString(cell.getValue()));
            }
        }
    }
    public Map<String, Object> stringToValue(String s){
        try {
            return _mapper.get().readValue(s, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            throw new RuntimeException("Unable to convert string to metadata: " + s);
        }
    }
    public String valueToString(Map<String, Object> value) {
        try {
            return _mapper.get().writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException("Unable to convert metadata to string: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnrichmentValue that = (EnrichmentValue) o;

        return getMetadata() != null ? getMetadata().equals(that.getMetadata()) : that.getMetadata() == null;

    }

    @Override
    public int hashCode() {
        return getMetadata() != null ? getMetadata().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "EnrichmentValue{" +
                "metadata=" + metadata +
                '}';
    }
}
