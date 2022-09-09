package com.cloudera.parserchains.core.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringEscapeUtils;

@UtilityClass
public final class StringUtils {

    public static char getFirstChar(String delimiter) {
        return StringEscapeUtils.unescapeJava(delimiter).charAt(0);
    }

}
