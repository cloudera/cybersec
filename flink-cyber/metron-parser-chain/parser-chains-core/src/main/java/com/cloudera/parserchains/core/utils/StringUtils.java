package com.cloudera.parserchains.core.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringEscapeUtils;

@UtilityClass
public final class StringUtils {

    public static char getFirstChar(String delimiter) {
        return unescapeJava(delimiter).charAt(0);
    }

    public static String unescapeJava(String text) {
        return StringEscapeUtils.unescapeJava(text);
    }

}
