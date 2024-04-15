package com.cloudera.service.common;


import org.junit.Test;

import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UtilsTest {

    // Sample enum for testing
    enum Color {
        RED, GREEN, BLUE
    }

    @Test
    public void testValueFromStringExactMatch() {
        Function<Color, String> nameFunction = color -> color.name().toLowerCase();

        Color result = Utils.getEnumFromString("red", Color.class, nameFunction);
        assertThat(result).isEqualTo(Color.RED);
    }

    @Test
    public void testValueFromStringCaseInsensitiveMatch() {
        Function<Color, String> nameFunction = color -> color.name().toLowerCase();

        Color result = Utils.getEnumFromString("gReEn", Color.class, nameFunction);
        assertThat(result).isEqualTo(Color.GREEN);
    }

    @Test
    public void testValueFromStringNoMatch() {
        Function<Color, String> nameFunction = color -> color.name().toLowerCase();

        Color result = Utils.getEnumFromString("black", Color.class, nameFunction);
        assertThat(result).isNull();
    }

    @Test
    public void testValueFromStringNullInput() {
        Function<Color, String> nameFunction = color -> color.name().toLowerCase();

        Color result = Utils.getEnumFromString(null, Color.class, nameFunction);
        assertThat(result).isNull();
    }

    @Test
    public void testValueFromStringCustomNameFunction() {
        Function<Color, String> customNameFunction = color -> {
            switch (color) {
                case RED:
                    return "Crimson";
                case GREEN:
                    return "Emerald";
                case BLUE:
                    return "Sapphire";
                default:
                    return "";
            }
        };

        Color result = Utils.getEnumFromString("Emerald", Color.class, customNameFunction);
        assertThat(result).isEqualTo(Color.GREEN);
    }
}
