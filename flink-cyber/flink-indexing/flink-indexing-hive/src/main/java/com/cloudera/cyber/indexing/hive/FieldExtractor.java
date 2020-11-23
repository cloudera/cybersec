package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.types.Row;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.flink.table.types.utils.TypeConversions.fromLegacyInfoToDataType;

public class FieldExtractor extends RichMapFunction<Message, Row> {

    List<String> listFields;
    protected static final List<String> headerFields = Arrays.asList("id", "ts", "message", "fields");
    protected static final List<String> noMapFields = Arrays.asList("id", "ts", "message");
    protected static final List<String> footerFields = Arrays.asList("source", "dt", "hr");
    private boolean includeFieldMap = false;

    private static final TypeInformation<?>[] ALL_BASE = {
            Types.STRING, // id
            Types.LONG, // ts
            Types.STRING, // message
            Types.MAP(Types.STRING, Types.STRING) // fields
    };
    private static final TypeInformation<?>[] NOMAP_BASE = {
            Types.STRING, // id
            Types.LONG, // ts
            Types.STRING // message
    };

    public FieldExtractor(List<String> listFields, boolean includeFieldMap) {
        this.listFields = listFields.stream().filter(n -> !(
                FieldExtractor.headerFields.contains(n) ||
                        FieldExtractor.footerFields.contains(n))
        ).collect(toList());
        this.includeFieldMap = includeFieldMap;
    }

    public List<String> fieldNames() {
        return Stream.of(includeFieldMap ?
                        headerFields.stream() :
                        noMapFields.stream(),
                listFields.stream(),
                footerFields.stream()).flatMap(s -> s)
                .collect(toList());
    }

    @Override
    public Row map(Message m) {
        LocalDateTime time = Instant.ofEpochMilli(m.getTs()).atOffset(ZoneOffset.UTC)
                .toLocalDateTime();
        Row output = Row.of(Stream.of(
                includeFieldMap ?
                        Stream.of(m.getId(), m.getTs(), m.getMessage(), flattenToStrings(m.getExtensions(), listFields)) :
                        Stream.of(m.getId(), m.getTs(), m.getMessage()),
                listFields.stream().map(field -> m.getExtensions().get(field)),
                Stream.of(m.getSource(), date(time), hour(time))
        ).flatMap(s -> s).toArray());
        return output;
    }

    private String hour(LocalDateTime time) {
        return String.valueOf(time.getHour());
    }

    String date(LocalDateTime time) {
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * TODO - properly recurse this.
     *
     * @param extensions
     * @param filter
     * @return
     */
    private static Map<String, String> flattenToStrings(Map<String, String> extensions, List<String> filter) {
        return extensions.entrySet().stream()
                .filter(e -> !filter.contains(e.getKey().toString()))
                .collect(toMap(
                        k -> k.getKey().toString(),
                        v -> v.getValue().toString())
                );
    }

    public TableSchema tableSchema() {
        TypeInformation[] types = type().getGenericParameters().values().toArray(new TypeInformation[]{});
        return TableSchema.builder().fields(
                fieldNames().toArray(new String[0]),
                fromLegacyInfoToDataType(types))
                .build();
    }

    public TypeInformation<Row> type() {
        String[] names = fieldNames().toArray(new String[fieldNames().size()]);

        TypeInformation<?>[] baseTypes = includeFieldMap ? ALL_BASE : NOMAP_BASE;
        Stream<TypeInformation<?>> typeInformationStream = listFields.stream().map(n -> Types.STRING);
        List<TypeInformation<?>> allTypes =
                Stream.of(
                        Stream.of(baseTypes),
                        typeInformationStream,
                        Stream.of(Types.STRING, Types.STRING, Types.STRING))
                        .flatMap(s -> s).collect(toList());

        return Types.ROW_NAMED(names, allTypes.toArray(new TypeInformation<?>[allTypes.size()]));
    }
}
