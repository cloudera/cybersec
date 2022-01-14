/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.hbase;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Allows the user to specify the projection criteria.
 * If only columnFamily is specified all columns from that family will be returned.
 * If a column is specified only that column from that family will be returned.
 *
 * Original code based on the Apache Storm project. See
 * https://github.com/apache/storm/tree/master/external/storm-hbase.
 */
public class HBaseProjectionCriteria implements Serializable {

  private List<byte[]> columnFamilies;
  private List<ColumnMetaData> columns;

  public static class ColumnMetaData implements Serializable {

    private byte[]  columnFamily;
    private byte[] qualifier;

    public ColumnMetaData(String columnFamily, String qualifier) {
      this.columnFamily = columnFamily.getBytes(StandardCharsets.UTF_8);
      this.qualifier = qualifier.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] getColumnFamily() {
      return columnFamily;
    }

    public byte[] getQualifier() {
      return qualifier;
    }
  }

  public HBaseProjectionCriteria() {
    columnFamilies = Lists.newArrayList();
    columns = Lists.newArrayList();
  }

  /**
   * all columns from this family will be included as result of HBase lookup.
   * @param columnFamily
   * @return column family along with all of its columns from an HBase lookup
   */
  public HBaseProjectionCriteria addColumnFamily(String columnFamily) {
    this.columnFamilies.add(columnFamily.getBytes(StandardCharsets.UTF_8));
    return this;
  }

  /**
   * Only this column from the the columnFamily will be included as result of HBase lookup.
   * @param column
   * @return a particular column from a column family as part of an HBase lookup
   */
  public HBaseProjectionCriteria addColumn(ColumnMetaData column) {
    this.columns.add(column);
    return this;
  }

  public List<ColumnMetaData> getColumns() {
    return columns;
  }

  public List<byte[]> getColumnFamilies() {
    return columnFamilies;
  }
}
