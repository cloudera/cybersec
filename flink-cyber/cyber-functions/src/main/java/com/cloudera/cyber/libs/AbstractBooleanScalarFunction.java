package com.cloudera.cyber.libs;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.table.functions.ScalarFunction;

public class AbstractBooleanScalarFunction extends ScalarFunction  {

    public TypeInformation<?> getResultType(Class<?>[] signature) {
        return Types.BOOLEAN;
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

}
