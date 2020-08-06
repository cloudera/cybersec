package com.cloudera.cyber.caracal;

import com.cloudera.cyber.parser.ParserChainMap;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
public class SplitConfig implements Serializable {

    private String topic;
    private String splitPath;
    private String headerPath;
    private String timestampField;
    @Builder.Default private ParserChainSchema chainSchema = new ParserChainSchema();


    /**
     * An optional function (javascript to apply to the timestamp)
     */
    @Builder.Default
    private String timestampFunction = "";

    private SplittingFlatMapFunction.TimestampSource timestampSource;
}
