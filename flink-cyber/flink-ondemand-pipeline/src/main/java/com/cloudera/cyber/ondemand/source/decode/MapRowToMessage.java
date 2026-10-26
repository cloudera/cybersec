package com.cloudera.cyber.ondemand.source.decode;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.types.Row;

public class MapRowToMessage extends RichMapFunction<Row, Message> implements ResultTypeQueryable<Message> {

    @Override
    public TypeInformation<Message> getProducedType() {
        return TypeInformation.of(Message.class);
    }

    @Override
    public Message map(Row row) throws Exception {
        return null;
    }
}
