package uk.ac.ucl.rits.inform.informdb.smart_data_elements;

/**
 * Possible types for an SDE, as defined in Clarity ZC_DATA_TYPE.
 * Does this enum belong here or in Interchange?
 */
public enum SmartDataElementType {
    STRING("String"),
    NUMBER("Number"),
    DATE("Date"),
    TIME("Time"),
    CATEGORY("Category"),
    BOOLEAN("Boolean"),
    DATABASE("Database"),
    ELEMENT_ID("Element ID"),
    IMAGE("Image"),
    TIME_WITH_UNIT("Time with unit"),
    INSTANT_UTC("Instant (UTC - Coordinated Universal Time)"),
    HIERARCHY("Hierarchy");

    private String name;

    SmartDataElementType(String name) {
        this.name = name;
    }
}
