package com.cloudera.cyber.commands;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.flink.HasHeaders;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
public class EnrichmentCommand extends Command<EnrichmentEntry> implements HasHeaders {

    public static final Schema SCHEMA$ = SchemaBuilder
            .record(EnrichmentCommand.class.getName())
            .namespace(EnrichmentCommand.class.getPackage().getName())
            .fields()
            .name("type").type(Schema.createEnum(CommandType.class.getName(),
                    "",
                    CommandType.class.getPackage().getName(),
                    Arrays.stream(CommandType.values()).map(v -> v.name()).collect(Collectors.toList()))).noDefault()
            .name("payload").type().optional().type(EnrichmentEntry.SCHEMA$)
            .name("headers").type(Schema.createMap(Schema.create(Schema.Type.STRING))).noDefault()
            .endRecord();

    protected EnrichmentCommand(EnrichmentCommandBuilder<?, ?> b) {
        super(b);
    }

    public static EnrichmentCommandBuilder<?, ?> builder() {
        return new EnrichmentCommandBuilderImpl();
    }

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    };

    public static Schema getClassSchema() {
        return SCHEMA$;
    }

    public static abstract class EnrichmentCommandBuilder<C extends EnrichmentCommand, B extends EnrichmentCommandBuilder<C, B>> extends CommandBuilder<EnrichmentEntry, C, B> {
        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "EnrichmentCommand.EnrichmentCommandBuilder(super=" + super.toString() + ")";
        }
    }

    private static final class EnrichmentCommandBuilderImpl extends EnrichmentCommandBuilder<EnrichmentCommand, EnrichmentCommandBuilderImpl> {
        private EnrichmentCommandBuilderImpl() {
        }

        protected EnrichmentCommand.EnrichmentCommandBuilderImpl self() {
            return this;
        }

        public EnrichmentCommand build() {
            return new EnrichmentCommand(this);
        }
    }
}
