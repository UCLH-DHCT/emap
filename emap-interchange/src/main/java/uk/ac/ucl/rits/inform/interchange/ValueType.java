package uk.ac.ucl.rits.inform.interchange;

/**
 * Value type that has a toString of it's mime type.
 * If no mime type, a custom value is given.
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
