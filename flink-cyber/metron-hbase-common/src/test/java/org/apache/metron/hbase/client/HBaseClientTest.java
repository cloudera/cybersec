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

package org.apache.metron.hbase.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.HBaseProjectionCriteria;
import org.apache.metron.hbase.TableProvider;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * Tests the HBaseClient
 */
public class HBaseClientTest {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String tableName = "table";

  private static HBaseTestingUtility util;
  private static HBaseClient client;
  private static Table table;
  private static Admin admin;
  private static final byte[] cf = Bytes.toBytes("cf");
  private static final byte[] column = Bytes.toBytes("column");
  byte[] rowKey1;
  byte[] rowKey2;
  byte[] value1 = Bytes.toBytes("value1");
  byte[] value2 = Bytes.toBytes("value2");
  ColumnList cols1;
  ColumnList cols2;


  public static void startHBase() throws Exception {
    Configuration config = HBaseConfiguration.create();
    config.set("hbase.master.hostname", "localhost");
    config.set("hbase.regionserver.hostname", "localhost");
    util = new HBaseTestingUtility();
    util.startMiniCluster();
    admin = util.getHBaseAdmin();
    // create the table
    table = util.createTable(TableName.valueOf(tableName), cf);
    util.waitTableEnabled(table.getName());
    // setup the client
    LOG.error("creating client");
    client = new HBaseClient((c,t) -> table, table.getConfiguration(), tableName);
    LOG.error("client created");
  }


  public static void stopHBase() throws Exception {
    util.deleteTable(TableName.valueOf(tableName));
    util.shutdownMiniCluster();
    util.cleanupTestDir();
  }


  public void clearTable() throws Exception {
    List<Delete> deletions = new ArrayList<>();
    for(Result r : table.getScanner(new Scan())) {
      deletions.add(new Delete(r.getRow()));
    }
    table.delete(deletions);
  }


  public void setupTuples() {
    rowKey1 = Bytes.toBytes("rowKey1");
    cols1 = new ColumnList();
    cols1.addColumn(cf, column, value1);

    rowKey2 = Bytes.toBytes("rowKey2");
    cols2 = new ColumnList();
    cols2.addColumn(cf, column, value2);
  }

  /**
   * Should be able to read/write a single row.
   */
  @Test
  public void testWrite() throws Exception {

    startHBase();
    setupTuples();
    // add a tuple to the batch
    client.addMutation(rowKey1, cols1, Durability.SYNC_WAL);
    client.mutate();

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(Bytes.toString(cf));

    // read back the tuple
    client.addGet(rowKey1, criteria);
    Result[] results = client.getAll();
    Assert.assertEquals(1, results.length);

    // validate
    assertEquals(1, results.length);
    assertArrayEquals(rowKey1, results[0].getRow());
    assertArrayEquals(value1, results[0].getValue(cf, column));
    stopHBase();
  }


}
