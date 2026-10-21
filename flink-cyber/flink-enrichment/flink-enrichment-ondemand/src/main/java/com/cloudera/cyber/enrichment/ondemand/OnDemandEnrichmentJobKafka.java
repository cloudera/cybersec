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

package com.cloudera.cyber.enrichment.ondemand;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.EnrichmentJobKafka;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiJobFactory;
import com.cloudera.cyber.scoring.ScoredMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.util.Preconditions;

import java.util.Arrays;
import java.util.List;

/**
 * Re-enriches a bounded set of already indexed log events and writes the result back through the
 * Hive table api indexer.
 *
 * <p>The pipeline is:
 * <ol>
 *   <li>read the log events table between the configured start and end timestamps for one or more
 *       source values;</li>
 *   <li>map every table row back to a {@link Message};</li>
 *   <li>run the shared {@code flink-enrichment-combined} enrichment and scoring chain;</li>
 *   <li>hand the enriched {@link ScoredMessage} stream to the {@code flink-indexing-hive}
 *       {@link TableApiAbstractJob}.</li>
 * </ol>
 *
 * <p>The enrichment command, threatq and scoring rule inputs are still read from Kafka, exactly as the
 * combined enrichment job does, because the broadcast lookup state and the scoring rules have to be
 * loaded before the events can be enriched.
 */
@Slf4j
public class OnDemandEnrichmentJobKafka extends EnrichmentJobKafka {

  private static final String PARAMS_TABLE = "logevents.table";
  private static final String PARAMS_TS_COLUMN = "logevents.ts-column";
  private static final String PARAMS_SOURCE_COLUMN = "logevents.source-column";
  private static final String PARAMS_START_TS = "logevents.start-ts";
  private static final String PARAMS_END_TS = "logevents.end-ts";
  private static final String PARAMS_SOURCES = "logevents.sources";

  private static final String DEFAULT_JOB_NAME = "On Demand Enrichment";
  private static final String DEFAULT_OUTPUT_CONNECTOR = "hive";
  private static final String PARAMS_OUTPUT_CONNECTOR = "flink.output-connector";

  public static void main(String[] args) throws Exception {
    Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
    ParameterTool params = Utils.getParamToolsFromProperties(args);
    StreamExecutionEnvironment pipeline = new OnDemandEnrichmentJobKafka().createPipeline(params);
    if (pipeline != null) {
      FlinkUtils.executeEnv(pipeline, DEFAULT_JOB_NAME, params);
    }
  }

  @Override
  public SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
    final String tableName = params.getRequired(PARAMS_TABLE);
    final String tsColumn = params.get(PARAMS_TS_COLUMN, LogEventQuery.DEFAULT_TS_COLUMN);
    final String sourceColumn = params.get(PARAMS_SOURCE_COLUMN, LogEventQuery.DEFAULT_SOURCE_COLUMN);
    final long startTs = params.getLong(PARAMS_START_TS, Long.MIN_VALUE);
    final long endTs = params.getLong(PARAMS_END_TS, Long.MAX_VALUE);
    final List<String> sources = Arrays.asList(params.getRequired(PARAMS_SOURCES).split(","));

    final String selectSql = LogEventQuery.buildSelectSql(tableName, tsColumn, sourceColumn, startTs, endTs, sources);
    log.info("Reading log events with SQL: {}", selectSql);

    final StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
    registerHiveCatalog(tableEnv, params);

    final Table events = tableEnv.sqlQuery(selectSql);
    final String[] fieldNames = events.getResolvedSchema().getColumnNames().toArray(new String[0]);

    return tableEnv.toDataStream(events)
        .map(new RowToMessage(fieldNames))
        .name("Row To Message")
        .uid("row-to-message");
  }

  @Override
  protected StreamExecutionEnvironment writeResults(StreamExecutionEnvironment env, ParameterTool params,
                                                    DataStream<ScoredMessage> results) {
    final String connectorName = params.get(PARAMS_OUTPUT_CONNECTOR, DEFAULT_OUTPUT_CONNECTOR);
    try {
      final TableApiAbstractJob job = TableApiJobFactory.getJobByConnectorName(connectorName, params, env, results);
      return job.startJob();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void registerHiveCatalog(StreamTableEnvironment tableEnv, ParameterTool params) {
    String name = "hive";
    String defaultDatabase = params.get("hive.dbname", "cyber");
    String hiveConfDir = params.get("hive.confdir", "/etc/hive/conf");

    HiveCatalog hive = new HiveCatalog(name, defaultDatabase, hiveConfDir);
    tableEnv.registerCatalog(name, hive);
    tableEnv.useCatalog(name);
  }
}
