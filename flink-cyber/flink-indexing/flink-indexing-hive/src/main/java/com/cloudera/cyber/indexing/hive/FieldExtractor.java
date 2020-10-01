package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.types.Row;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class FieldExtractor extends RichMapFunction<Message, Row> {

    List<String> listFields;
    protected static final List<String> headerFields = Arrays.asList("source", "id", "ts", "message", "fields");

    public FieldExtractor(List<String> listFields) {
        this.listFields = listFields.stream().filter(n -> !FieldExtractor.headerFields.contains(n)).collect(toList());
    }

    public List<String> fieldNames() {
        return Stream.concat(headerFields.stream(),
                listFields.stream()).collect(toList());
    }

    @Override
    public Row map(Message m) throws Exception {
        return Row.of(Stream.of(
                Stream.of(m.getId(), m.getTs(), m.getMessage(), flattenToStrings(m.getExtensions(), listFields)),
                listFields.stream().map(field -> m.getExtensions().get(field)),
                Stream.of(m.getSource())
        ).flatMap(s->s).toArray());
    }

    /**
     * TODO - properly recurse this.
     *
     * @param extensions
     * @param filter
     * @return
     */
    private static Map<String, String> flattenToStrings(Map<String, Object> extensions, List<String> filter) {
        return extensions.entrySet().stream()
                .filter(e -> !filter.contains(e.getKey()))
                .collect(toMap(
                        k -> k.getKey(),
                        v -> v.getValue().toString())
                );
    }

    public TypeInformation<Row> type() {
        String[] names = fieldNames().toArray(new String[fieldNames().size()]);

        TypeInformation<?>[] baseTypes = {
                Types.STRING, // id
                Types.SQL_TIMESTAMP, // ts
                Types.STRING, // message
                Types.MAP(Types.STRING, Types.STRING) //fields
        };
        Stream<TypeInformation<?>> typeInformationStream = listFields.stream().map(n -> Types.STRING);

        List<TypeInformation<?>> allTypes = Stream.of(Stream.of(baseTypes), typeInformationStream, Stream.of(Types.STRING)).flatMap(s->s).collect(toList());
        return Types.ROW_NAMED(names, allTypes.toArray(new TypeInformation<?>[allTypes.size()]));
    }
}
