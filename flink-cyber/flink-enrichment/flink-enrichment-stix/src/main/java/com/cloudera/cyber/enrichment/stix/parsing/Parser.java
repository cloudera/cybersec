package com.cloudera.cyber.enrichment.stix.parsing;

import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.types.ObjectTypeHandler;
import com.cloudera.cyber.enrichment.stix.parsing.types.ObjectTypeHandlers;
import com.google.common.base.Splitter;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.joda.time.DateTime;
import org.mitre.cybox.common_2.BaseObjectPropertyType;
import org.mitre.cybox.common_2.ConditionApplicationEnum;
import org.mitre.cybox.common_2.ConditionTypeEnum;
import org.mitre.cybox.common_2.ObjectPropertiesType;
import org.mitre.cybox.cybox_2.ObjectType;
import org.mitre.cybox.cybox_2.Observable;
import org.mitre.cybox.cybox_2.Observables;
import org.mitre.stix.indicator_2.Indicator;
import org.mitre.stix.stix_1.IndicatorsType;
import org.mitre.stix.stix_1.STIXPackage;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
public class Parser extends RichFlatMapFunction<String, ParsedThreatIntelligence> {

    private Configuration config;

    @Override
    public void open(Configuration parameters) throws Exception {
        this.config = parameters;
        super.open(parameters);
    }

    @Override
    public void flatMap(String s, Collector<ParsedThreatIntelligence> collector) {
        STIXPackage stixPackage = STIXPackage.fromXMLString(s);

        Observables observables = stixPackage.getObservables();
        IndicatorsType indicators = stixPackage.getIndicators();


        if (observables != null) {
            observables.getObservables().stream()
                    .map(Observable::getObject)
                    .map(ObjectType::getProperties)
                    .flatMap(p -> {
                        ObjectTypeHandler handler = ObjectTypeHandlers.getHandlerByInstance(p);
                        Stream<ThreatIntelligence.Builder> extract = handler.extract(p, config.toMap());
                        return extract.map(b -> b.setTs(DateTime.now()));
                    })
                    .forEach(t -> collector.collect(
                            ParsedThreatIntelligence.newBuilder()
                                    .setSource(s)
                                    .setThreatIntelligence(t.build())
                                    .build()));
        }
        if (indicators != null) {
            indicators.getIndicators().stream()
                    .flatMap(i -> {
                        Indicator indicator = (Indicator) i;
                        Observable observable = indicator.getObservable();
                        ObjectPropertiesType p = observable.getObject().getProperties();
                        ObjectTypeHandler handler = ObjectTypeHandlers.getHandlerByInstance(p);
                        Stream<ThreatIntelligence.Builder> out = handler.extract(p, config.toMap());

                        XMLGregorianCalendar time = i.getTimestamp();

                        Instant timestamp = time == null ?
                                Instant.now() :
                                i.getTimestamp().toGregorianCalendar().toZonedDateTime().toInstant();

                        return out.map(t -> t
                                .setStixReference(indicator.getId().toString())
                                .setTs(org.joda.time.Instant.ofEpochMilli(timestamp.toEpochMilli()).toDateTime()));
                    })
                    .filter(Objects::nonNull)
                    .forEach(t -> collector.collect(
                            ParsedThreatIntelligence.newBuilder()
                                    .setSource(s)
                                    .setThreatIntelligence(t.setId(UUID.randomUUID().toString()).build())
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
