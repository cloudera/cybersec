package com.cloudera.parserchains.core.model.define;

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * Uniquely identifies a parser.
 */
public class ParserID implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonValue
    private final String id;
    
    /**
     * Creates a new {@link ParserID} from a Parser class.
     * @param clazz The Parser class.
     * @return
     */
    public static ParserID of(Class<?> clazz) {
        return new ParserID(clazz.getCanonicalName());
    }

    /**
     * Returns the {@link ParserID} used to identify a router.
     */
    public static ParserID router() {
        return new ParserID("Router");
    }

    /**
     * Private constructor.  See {@link ParserID#of(Class)}.
     */
    private ParserID(String id) {
        this.id = Objects.requireNonNull(id, "A valid ID is required.");
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParserID parserID = (ParserID) o;
        return new EqualsBuilder()
                .append(id, parserID.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ParserID{" +
                "id='" + id + '\'' +
                '}';
    }
}
