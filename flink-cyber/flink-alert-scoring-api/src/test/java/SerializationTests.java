import com.cloudera.cyber.rules.RuleType;
import com.cloudera.cyber.scoring.Scores;
import com.cloudera.cyber.scoring.ScoringRule;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.apache.flink.util.InstantiationUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SerializationTests {

    @Test
    public void testScore() throws IOException {
        Scores test = Scores.builder().reason("test").score(1.0).build();
        Scores out = test(test);
        assertThat(out, equalTo(test));
    }

    @Test
    @Ignore
    public void testScoringRule() throws IOException {
        ScoringRule test = ScoringRule.builder()
                .enabled(true)
                .name("test")
                .order(1)
                .ruleScript("test()")
                .tsStart(Instant.MIN)
                .tsEnd(Instant.MAX)
                .type(RuleType.JS)
                .build();

        fail("Cannot serialize because of base class");
    }


    public static <T extends SpecificRecordBase> T test(T obj) throws IOException {
        Class cls = obj.getClass();
        AvroTypeInfo<T> ti = new AvroTypeInfo<T>(cls);
        TypeSerializer<T> serializer = ti.createSerializer(new ExecutionConfig());

        byte[] bytes = InstantiationUtil.serializeToByteArray(serializer, obj);
        T out = InstantiationUtil.deserializeFromByteArray(serializer, bytes);

        assertThat(out, notNullValue());
        return out;
    }

    @Test
    public void schemaTest() {
        Schema schema = ReflectData.get().getSchema(ScoringRule.class);
        assertThat(schema, notNullValue());
        assertThat(schema, equalTo(ScoringRule.SCHEMA$));
    }
}
