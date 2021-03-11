package uk.ac.ucl.rits.inform.interchange;

/**
 * Value type that has a toString of it's mime type.
 * <p>
 * Used for flowsheet values, as well as Lab Results (which make use fo mime type).
 * If no mime type, a custom value is given.
 */
public enum ValueType {
    /**
     * Numeric value.
     * (Doesn't exist as mime type so custom type used)
     */
    NUMERIC("numeric/plain"),
    /**
     * Text value.
     */
    TEXT("text/plain"),
    /**
     * Date value.
     * (Doesn't exist as mime type so custom type used)
     */
    DATE("temporal/date"),
    /**
     * Time value.
     * (Doesn't exist as mime type so custom type used)
     */
    TIME("temporal/time"),
    /**
     * PDF value.
     */
    PDF("application/pdf"),
    /**
     * Lab Isolate.
     */
    LAB_ISOLATE("link/lab_isolate");

    private final String mimeType;

    ValueType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        return mimeType;
    }
}
