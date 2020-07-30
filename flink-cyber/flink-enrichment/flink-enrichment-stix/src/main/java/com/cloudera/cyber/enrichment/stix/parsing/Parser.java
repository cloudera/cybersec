package com.cloudera.cyber.enrichment.stix.parsing;

import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.types.ObjectTypeHandler;
import com.cloudera.cyber.enrichment.stix.parsing.types.ObjectTypeHandlers;
import com.google.common.base.Splitter;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.mitre.cybox.common_2.BaseObjectPropertyType;
import org.mitre.cybox.common_2.ConditionApplicationEnum;
import org.mitre.cybox.common_2.ConditionTypeEnum;
import org.mitre.cybox.common_2.ObjectPropertiesType;
import org.mitre.cybox.cybox_2.Observable;
import org.mitre.cybox.cybox_2.Observables;
import org.mitre.stix.indicator_2.Indicator;
import org.mitre.stix.stix_1.IndicatorsType;
import org.mitre.stix.stix_1.STIXPackage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Parser extends RichFlatMapFunction<String, ParsedThreatIntelligence> {

    private Configuration config;

    @Override
    public void open(Configuration parameters) throws Exception {
        this.config = parameters;
        super.open(parameters);
    }

    @Override
    public void flatMap(String s, Collector<ParsedThreatIntelligence> collector) throws Exception {
        STIXPackage stixPackage = STIXPackage.fromXMLString(s);

        Observables observables = stixPackage.getObservables();
        IndicatorsType indicators = stixPackage.getIndicators();


        if (observables != null) {
            observables.getObservables().stream()
                    .map(o -> o.getObject())
                    .map(o -> o.getProperties())
                    .flatMap(p -> {
                        ObjectTypeHandler handler = ObjectTypeHandlers.getHandlerByInstance(p);
                        Stream<ThreatIntelligence.ThreatIntelligenceBuilder> extract = handler.extract(p, config.toMap());
                        return extract
                                .map(b -> b.ts(Instant.now().toEpochMilli()));
                    })
                    .forEach(t -> collector.collect(
                            ParsedThreatIntelligence.builder()
                                    .source(s)
                                    .threatIntelligence(t.build())
                                    .build()));
        }
        if (indicators != null) {
            indicators.getIndicators().stream()
                    .flatMap(i -> {
                        Indicator indicator = (Indicator) i;
                        Observable observable = indicator.getObservable();
                        ObjectPropertiesType p = observable.getObject().getProperties();
                        ObjectTypeHandler handler = ObjectTypeHandlers.getHandlerByInstance(p);
                        Stream<ThreatIntelligence.ThreatIntelligenceBuilder> out = handler.extract(p, config.toMap());
                        Stream<ThreatIntelligence.ThreatIntelligenceBuilder> threatIntelligenceBuilderStream = out.map(t -> t.stixReference(indicator.getId().toString()));
                        return threatIntelligenceBuilderStream;
                    })
                    .filter(Objects::nonNull)
                    .forEach(t -> collector.collect(
                            ParsedThreatIntelligence.builder()
                                    .source(s)
                                    .threatIntelligence(t.build())
                                    .build()));
        }
    }

    /**
     * Extract values that can be applied with a simple equality comparator
     * <p>
     * Note that for more complex STIX patterns a separate loader, index and rules engine will be used
     * <p>
     * See: https://github.com/apache/metron/blob/2ee6cc7e0b448d8d27f56f873e2c15a603c53917/metron-platform/metron-data-management/src/main/java/org/apache/metron/dataloads/extractor/stix/StixExtractor.java#L107
     *
     * @param value
     * @return
     */
    public static Iterable<String> split(BaseObjectPropertyType value) {
        final ConditionTypeEnum condition = value.getCondition() == null ? ConditionTypeEnum.EQUALS : value.getCondition();
        final ConditionApplicationEnum applyCondition = value.getApplyCondition();
        List<String> tokens = new ArrayList<>();
        if (condition == ConditionTypeEnum.EQUALS && applyCondition == ConditionApplicationEnum.ANY) {
            String delim = value.getDelimiter();
            String line = value.getValue().toString();
            if (delim != null) {
                for (String token : Splitter.on(delim).split(line)) {
                    tokens.add(token);
                }
            } else {
                tokens.add(line);
            }
        }
        return tokens;
    }


}
