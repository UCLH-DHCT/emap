package uk.ac.ucl.rits.inform.interchange;

/**
 * Value type that has a toString of it's mime type.
 * <p>
 * Used for flowsheet values, as well as Lab Results. If no mime type, a custom value is given.
 */
public enum ValueType {
    /**
     * Numeric value.
     */
    NUMERIC("numeric/plain"),
    /**
     * Text value.
     */
    TEXT("text/plain"),
    /**
     * Date value.
     */
    DATE("temporal/date"),
    /**
     * Time value.
     */
    TIME("temporal/time"),
    /**
     * PDF value.
     */
    PDF("application/pdf");

    private String mimeType;

    ValueType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        return mimeType;
    }
}
