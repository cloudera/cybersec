package com.cloudera.cyber.libs;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.table.functions.ScalarFunction;

public class AbstractMapScalarFunction extends ScalarFunction {

    public TypeInformation<?> getResultType(Class<?>[] signature) {
        return Types.MAP(Types.STRING, Types.STRING);
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }
}
