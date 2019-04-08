package uk.ac.ucl.rits.inform.informdb;

import java.util.HashMap;
import java.util.Map;

/**
 * This maps programming used attributes names to Attribute shortnames.
 *
 * @author UCL RITS
 *
 */
public enum AttributeKeyMap {

    /**
     * The first name of a person.
     */
    FIRST_NAME("F_NAME"),
    /**
     * Middle names separated by ,s.
     */
    MIDDLE_NAMES("M_NAME"),
    /**
     * Family name.
     */
    FAMILY_NAME("L_NAME"),
    /**
     * Naming Grouper.
     */
    NAME_FACT("NAMING"),
    /**
     * Hospital visit grouper.
     */
    HOSPITAL_VISIT("HOSP_VISIT"),
    /**
     * Bed visit grouper.
     */
    BED_VISIT("BED_VISIT"),
    /**
     * Arrival time as a date time.
     */
    ARRIVAL_TIME("ARRIVAL_TIME"),
    /**
     * Discharge time as a date time.
     */
    DISCHARGE_TIME("DISCH_TIME"),
    /**
     * Location as a String.
     */
    LOCATION("LOCATION"),
    /**
     * Parent visit.
     */
    PARENT_VISIT("VISIT_PAREN");

    private String shortname;

    /**
     * Create an Enum value with a specified short name used in the attributes
     * table.
     *
     * @param shortname the short name used in the database
     */
    AttributeKeyMap(String shortname) {
        this.shortname = shortname;
    }

    /**
     * Get the short name for this attribute.
     *
     * @return The short name
     */
    public String getShortname() {
        return this.shortname;
    }
}
