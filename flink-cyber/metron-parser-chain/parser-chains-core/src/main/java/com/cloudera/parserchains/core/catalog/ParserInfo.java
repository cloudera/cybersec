package com.cloudera.parserchains.core.catalog;

import com.cloudera.parserchains.core.Parser;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

/**
 * Describes a {@link Parser} that was discovered using a {@link ParserCatalog}.
 */
public class ParserInfo {
    private final String name;
    private final String description;
    private final Class<? extends Parser> parserClass;

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Private constructor.  See {@link Builder#builder()}.
     */
    private ParserInfo(String name, String description, Class<? extends Parser> parserClass) {
        this.name = Objects.requireNonNull(name, "A name is required.");
        this.description = Objects.requireNonNull(description, "A description is required.");
        this.parserClass = Objects.requireNonNull(parserClass, "A parserClass is required.");
    }

    /**
     * Returns the name of the parser.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the parser.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the implementation class of the parser.
     */
    public Class<? extends Parser> getParserClass() {
        return parserClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParserInfo that = (ParserInfo) o;
        return new EqualsBuilder()
                .append(name, that.name)
                .append(description, that.description)
                .append(parserClass, that.parserClass)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(description)
                .append(parserClass)
                .toHashCode();
    }

    public static class Builder {
        private String name;
        private String description;
        private Class<? extends Parser> parserClass;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parserClass(Class<? extends Parser> parserClass) {
            this.parserClass = parserClass;
            return this;
        }

        public ParserInfo build() {
            return new ParserInfo(name, description, parserClass);
        }
    }
}
