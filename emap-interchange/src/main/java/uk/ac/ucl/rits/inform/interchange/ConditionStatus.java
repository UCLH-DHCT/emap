package uk.ac.ucl.rits.inform.interchange;

/**
 * Status of a patient condition (e.g. problem or infection)
 */
public enum ConditionStatus {
    /**
     * Active.
     */
    ACTIVE("ACTIVE"),
    /**
     * Save.
     */
    SAVE("SAVE"),
    /**
     * Resolved.
     */
    RESOLVED("RESOLVED"),
    /**
     * Deleted.
     */
    DELETED("DELETED"),
    /**
     * Ignore.
     */
    IGNORE("IGNORE");
    // are all of these statuses used?

    private final String value;

    ConditionStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
