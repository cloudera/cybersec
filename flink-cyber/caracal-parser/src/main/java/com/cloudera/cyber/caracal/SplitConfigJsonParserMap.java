package com.cloudera.cyber.caracal;

import com.cloudera.parserchains.core.utils.JSONUtils;
import org.apache.flink.api.common.functions.RichMapFunction;

public class SplitConfigJsonParserMap extends RichMapFunction<String, SplitConfig> {
    @Override
    public SplitConfig map(String configJson) throws Exception {
        return JSONUtils.INSTANCE.getMapper().readValue(configJson, SplitConfig.class);
    }
}
