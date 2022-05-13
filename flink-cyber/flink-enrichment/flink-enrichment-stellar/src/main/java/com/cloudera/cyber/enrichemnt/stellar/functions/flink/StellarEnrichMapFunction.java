package com.cloudera.cyber.enrichemnt.stellar.functions.flink;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichemnt.stellar.adapter.MetronGeoEnrichmentAdapter;
import com.cloudera.cyber.enrichment.geocode.IpGeoJob;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.adapters.stellar.StellarAdapter;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.parallel.EnrichmentStrategies;
import org.apache.metron.enrichment.parallel.EnrichmentStrategy;
import org.apache.metron.stellar.common.Constants;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.metron.stellar.common.Constants.STELLAR_CONTEXT_CONF;


@Slf4j
public class StellarEnrichMapFunction extends RichMapFunction<Message, Message> {
    private static final String GEO_ADAPTER_NAME = "geo";
    private static final String STELLAR_ADAPTER_NAME = "stellar";
    private static final Map<String, String> ADAPTER_NAME_FEATURE = ImmutableMap.of(GEO_ADAPTER_NAME, "stellar_geo_feature", STELLAR_ADAPTER_NAME, "stellar_feature");
    private final Map<String, String> stringEnrichmentConfigs;
    private final String geoDatabasePath;
    private final String asnDatabasePath;
    private static final String ENRICHMENT = "ENRICHMENT";
    private transient Map<String, SensorEnrichmentConfig> sensorEnrichmentConfigs;
    private transient Map<String, EnrichmentAdapter<CacheKey>> adapterMap;

    public StellarEnrichMapFunction(Map<String, String> configs, String geoDatabasePath, String asnDatabasePath) {
        this.stringEnrichmentConfigs = new HashMap<>(configs);
        this.geoDatabasePath = geoDatabasePath;
        this.asnDatabasePath = asnDatabasePath;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        sensorEnrichmentConfigs = initSensorConfigs();
        initAdapters();
    }

    private void initAdapters() {
        adapterMap = new HashMap<>();
        StellarAdapter stellarAdapter = new StellarAdapter().ofType(ENRICHMENT);
        stellarAdapter.initializeAdapter(null);
        adapterMap.put(STELLAR_ADAPTER_NAME, stellarAdapter);
        MetronGeoEnrichmentAdapter geoEnrichmentAdapter = new MetronGeoEnrichmentAdapter();
        geoEnrichmentAdapter.initializeAdapter(ImmutableMap.of(IpGeoJob.PARAM_GEO_DATABASE_PATH, geoDatabasePath));
        adapterMap.put(GEO_ADAPTER_NAME, geoEnrichmentAdapter);
    }

    private Map<String, SensorEnrichmentConfig> initSensorConfigs() throws java.io.IOException {
        Map<String, SensorEnrichmentConfig> result = new HashMap<>();
        for (Map.Entry<String, String> stringConfigEntry : stringEnrichmentConfigs.entrySet()) {
            EnrichmentConfigurations enrichmentConfigurations = new EnrichmentConfigurations();
            enrichmentConfigurations.updateGlobalConfig(ImmutableMap.of(IpGeoJob.PARAM_GEO_DATABASE_PATH, geoDatabasePath, IpGeoJob.PARAM_ASN_DATABASE_PATH, asnDatabasePath));
            SensorEnrichmentConfig sensorEnrichmentConfig;
            sensorEnrichmentConfig = JSONUtils.INSTANCE.load(stringConfigEntry.getValue(), SensorEnrichmentConfig.class);
            sensorEnrichmentConfig.getConfiguration().putIfAbsent(STELLAR_CONTEXT_CONF, initializeStellarContext(enrichmentConfigurations));
            enrichmentConfigurations.updateSensorEnrichmentConfig(ENRICHMENT, sensorEnrichmentConfig);
            result.put(stringConfigEntry.getKey(), sensorEnrichmentConfig);

        }
        return result;
    }

    private Context initializeStellarContext(EnrichmentConfigurations enrichmentConfigurations) {
        Context stellarContext = new Context.Builder()
                .with(Context.Capabilities.GLOBAL_CONFIG, enrichmentConfigurations::getGlobalConfig)
                .with(Context.Capabilities.STELLAR_CONFIG, enrichmentConfigurations::getGlobalConfig)
                .build();

        StellarFunctions.initialize(stellarContext);
        return stellarContext;
    }


    private Map<String, List<JSONObject>> generateTasks(JSONObject message, SensorEnrichmentConfig config
    ) {
        Map<String, List<JSONObject>> streamMessageMap = new HashMap<>();
        Map<String, Object> enrichmentFieldMap = EnrichmentStrategies.ENRICHMENT.getUnderlyingConfig(config).getFieldMap();
        Map<String, ConfigHandler> fieldToHandler = EnrichmentStrategies.ENRICHMENT.getUnderlyingConfig(config).getEnrichmentConfigs();

        Set<String> enrichmentTypes = new HashSet<>(enrichmentFieldMap.keySet());

        //the set of enrichments configured
        enrichmentTypes.addAll(fieldToHandler.keySet());

        //For each of these enrichment types, we're going to construct JSONObjects
        //which represent the individual enrichment tasks.
        for (String enrichmentType : enrichmentTypes) {
            Object fields = enrichmentFieldMap.get(enrichmentType);
            ConfigHandler retriever = fieldToHandler.get(enrichmentType);

            //How this is split depends on the ConfigHandler
            List<JSONObject> enrichmentObject = retriever.getType()
                    .splitByFields(message
                            , fields
                            , field -> ((EnrichmentStrategy) EnrichmentStrategies.ENRICHMENT).fieldToEnrichmentKey(enrichmentType, field)
                            , retriever
                    );
            streamMessageMap.put(enrichmentType, enrichmentObject);
        }
        return streamMessageMap;
    }

    @Override
    public Message map(Message message) {
        HashMap<String, String> extensions = new HashMap<>(message.getExtensions());
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();

        String source = message.getSource();
        SensorEnrichmentConfig sensorEnrichmentConfig = sensorEnrichmentConfigs.get(source);
        if (sensorEnrichmentConfig != null) {
            extensions.put(Constants.SENSOR_TYPE, source);
            extensions.put(Constants.Fields.TIMESTAMP.getName(), String.valueOf(Instant.now().toEpochMilli()));
            JSONObject obj = new JSONObject(extensions);
            Map<String, String> tmpMap = new HashMap<>();
            Map<String, List<JSONObject>> tasks = generateTasks(obj, sensorEnrichmentConfig);
            for (Map.Entry<String, List<JSONObject>> task : tasks.entrySet()) {
                EnrichmentAdapter<CacheKey> adapter = adapterMap.get(task.getKey());
                for (JSONObject m : task.getValue()) {
                    collectData(sensorEnrichmentConfig, task, adapter, m, tmpMap, dataQualityMessages);
                }
            }
            return MessageUtils.enrich(message, tmpMap, dataQualityMessages);
        } else {
            return message;
        }
    }

    private void collectData(SensorEnrichmentConfig sensorEnrichmentConfig, Map.Entry<String, List<JSONObject>> task, EnrichmentAdapter<CacheKey> adapter, JSONObject m, Map<String, String> tmpMap, List<DataQualityMessage> dataQualityMessages) {
        for (Object fieldValue : m.entrySet()) {
            Map.Entry<String, Object> fieldValueEntry = (Map.Entry<String, Object>) fieldValue;
            String field = fieldValueEntry.getKey();
            Object value = fieldValueEntry.getValue();
            CacheKey cacheKey = new CacheKey(field, value, sensorEnrichmentConfig);
            try {
                JSONObject enrichmentResults = adapter.enrich(cacheKey);
                enrichmentResults.forEach((k,v) -> {
                    tmpMap.put((String)k, ObjectUtils.toString(v, "null") );
                });
            } catch (Exception e) {
                log.error("Error with " + task.getKey() + " failed: " + e.getMessage(), e);
                MessageUtils.addQualityMessage(dataQualityMessages, DataQualityMessageLevel.ERROR, "Error with " + task.getKey() + " failed: " + e.getMessage(), field, ADAPTER_NAME_FEATURE.get(task.getKey()));
            }
        }
    }
}
