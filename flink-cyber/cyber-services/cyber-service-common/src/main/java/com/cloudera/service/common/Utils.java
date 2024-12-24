package com.cloudera.service.common;

import java.util.Arrays;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class Utils {

    /**
     * Retrieves an enum constant of the specified enum type based on a case-insensitive search of a string representation,
     * using a custom mapping function to extract the string representation from the enum constant.
     *
     * @param <T>          the enum type
     * @param name         the string representation to search for
     * @param enumClass    the Class object representing the enum type
     * @param nameFunction a function that extracts the string representation from an enum constant
     * @return the enum constant matching the provided string representation, or null if not found
     * @throws NullPointerException if {@code name}, {@code enumClass}, or {@code nameFunction} is null
     */
    public static <T extends Enum<T>> T getEnumFromString(String name, Class<T> enumClass,
                                                          Function<T, String> nameFunction) {
        return Arrays.stream(enumClass.getEnumConstants())
                     .filter(type -> StringUtils.equalsIgnoreCase(name, nameFunction.apply(type)))
                     .findFirst()
                     .orElse(null);
    }

    public static <T extends Enum<T>> T getEnumFromStringContains(String name, Class<T> enumClass,
                                                                  Function<T, String> nameFunction) {
        return Arrays.stream(enumClass.getEnumConstants())
                     .filter(type -> StringUtils.containsIgnoreCase(name, nameFunction.apply(type)))
                     .findFirst()
                     .orElse(null);
    }
}
