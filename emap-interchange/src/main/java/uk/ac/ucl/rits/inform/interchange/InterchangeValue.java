package uk.ac.ucl.rits.inform.interchange;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Wrapper for data in interchange fields that can either be unknown or known.
 * @param <T> Interchange field data type.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class InterchangeValue<T> implements Serializable {
    private T value;
    private ResultStatus status;

    /**
     * Default constructor for serialisation.
     */
    public InterchangeValue() {
    }

    /**
     * @param status status to set.
     */
    private InterchangeValue(ResultStatus status) {
        if (status == ResultStatus.SAVE) {
            throw new IllegalArgumentException("InterchangeValue with no value set cannot have a status of SAVE");
        }
        this.status = status;
    }

    /**
     * Create an unknown Value.
     * @param <T> value type
     * @return InterchangeValue of unknown data
     */
    public static <T> InterchangeValue<T> unknown() {
        return new InterchangeValue<T>(ResultStatus.IGNORE);
    }

    /**
     * Create a delete value.
     * @param <T> value type
     * @return InterchangeValue of unknown data
     */
    public static <T> InterchangeValue<T> delete() {
        return new InterchangeValue<T>(ResultStatus.DELETE);
    }

    /**
     * Builds InterchangeValue class from hl7 value.
     * @param hl7Value value from HL7 message
     * @param <T>      type of the value
     * @return InterchangeValue class set with the correct status and data.
     */
    public static <T> InterchangeValue<T> buildFromHl7(T hl7Value) {
        if (hl7Value == null || hl7Value.equals("")) {
            return unknown();
        } else if (hl7Value.equals("\"\"")) {
            return delete();
        }
        return new InterchangeValue<>(hl7Value);
    }

    /**
     * Construct with a known value.
     * @param value of the field, cannot be null
     */
    public InterchangeValue(T value) {
        if (value == null) {
            throw new IllegalStateException("InterchangeValue with a status of SAVE can't have a null value set");
        }
        this.value = value;
        status = ResultStatus.SAVE;
    }

    /**
     * @return the known value.
     */
    public T get() {
        if (status == ResultStatus.IGNORE) {
            throw new IllegalStateException("InterchangeValue get method shouldn't be called when the result is unknown");
        }
        return value;
    }

    /**
     * @return if the value is unknown.
     */
    @JsonIgnore
    public boolean isUnknown() {
        return ResultStatus.IGNORE == status;
    }

    /**
     * @return if the value should be saved.
     */
    @JsonIgnore
    public boolean isSave() {
        return ResultStatus.SAVE == status;
    }


    /**
     * @return if the value should be deleted.
     */
    @JsonIgnore
    public boolean isDelete() {
        return ResultStatus.DELETE == status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InterchangeValue<?> interchangeValue = (InterchangeValue<?>) o;
        return Objects.equals(value, interchangeValue.value)
                && status == interchangeValue.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, status);
    }

    @Override
    public String toString() {
        return String.format("InterchangeValue{value=%s, status=%s}", value, status);
    }
}
