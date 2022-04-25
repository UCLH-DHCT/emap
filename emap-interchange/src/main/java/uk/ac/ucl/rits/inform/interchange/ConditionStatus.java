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
    RESOLVED("Resolved"),
    /**
     * Deleted.
     */
    DELETED("DELETED"),
    /**
     * Ignore.
     */
    IGNORE("IGNORE");
    // are all of these statuses used?

    private final String name;

    ConditionStatus(String name) {
        this.name = name;
    }

    /**
     * Find ConditionStatus by name.
     * @param name status name from hl7
     * @return ConditionStatus
     * @throws IllegalArgumentException if an unknown name is encountered
     */
    public static ConditionStatus findByHl7Value(String name) {
        for (ConditionStatus status : values()) {
            if (status.name.equals(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("ConditionStatus '%s' is not a known type", name));
    }
}
