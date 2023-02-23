package uk.ac.ucl.rits.inform.interchange;

/**
 * Record Status values from EPIC.
 */
public enum EpicRecordStatus {
    /**
     * Active.
     */
    ACTIVE("Active"),
    /**
     * Inactive.
     */
    INACTIVE("Inactive"),
    /**
     * Deleted.
     */
    DELETED("Deleted"),
    /**
     * Inactive and Deleted.
     */
    INACTIVE_AND_DELETED("Inactive and Deleted"),
    /**
     * Hidden.
     */
    HIDDEN("Hidden"),
    /**
     * Inactive and Hidden.
     */
    INACTIVE_AND_HIDDEN("Inactive and Hidden"),
    /**
     * Deleted and Hidden.
     */
    DELETED_AND_HIDDEN("Deleted and Hidden"),
    /**
     * Inactive Deleted and Hidden.
     */
    INACTIVE_DELETED_AND_HIDDEN("Inactive Deleted and Hidden");

    private final String name;

    EpicRecordStatus(String name) {
        this.name = name;
    }

    /**
     * Find EpicRecordStatus by name.
     * @param name name in EPIC
     * @return EpicRecordStatus
     * @throws IllegalArgumentException if an unknown name is encountered
     */
    public static EpicRecordStatus findByName(String name) {
        for (EpicRecordStatus recordStatus : values()) {
            if (recordStatus.name.equals(name)) {
                return recordStatus;
            }
        }
        throw new IllegalArgumentException(String.format("EpicRecordStatus %s is not a known type", name));
    }

    @Override
    public String toString() {
        return name;
    }
}
