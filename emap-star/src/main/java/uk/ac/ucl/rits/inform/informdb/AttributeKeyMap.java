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
    PARENT_VISIT("VISIT_PAREN"),
    /**
     * The NHS number associated with an encounter (and hopefully
     * an entire MRN).
     */
    NHS_NUMBER("NHS_NUMBER"),
    /**
     * General demographic grouper.
     */
    GENERAL_DEMOGRAPHIC("GENERAL_DEMO"),
    /**
     * Date of Birth.
     */
    DOB("DOB"),
    /**
     * Patient home postcode.
     */
    POST_CODE("POST_CODE"),
    /**
     * Sex.
     */
    SEX("SEX"),
    /**
     * Male.
     */
    MALE("MALE"),
    /**
     * Female.
     */
    FEMALE("FEMALE"),
    /**
     * Other, i.e. not a category we have explicitly recorded.
     */
    OTHER("OTHER"),
    /**
     * Date item is known to exist, but its value is unknown.
     */
    UNKNOWN("UNKNOWN"),
    /**
     * Fact type - is a path test result.
     */
    PATHOLOGY_TEST_RESULT("PATH_RESULT"),
    /**
     * Local code for the battery of tests (eg. FBCY)
     */
    PATHOLOGY_TEST_BATTERY_CODE("PATH_BATT_COD"),
    /**
     * Local description for the battery of tests.
     */
    PATHOLOGY_TEST_BATTERY_DESCRIPTION("PATH_BATT_DESC"),
    /**
     * Local code for the individual test.
     */
    PATHOLOGY_TEST_CODE("PATH_TEST_COD"),
    /**
     * Local description for the individual test.
     */
    PATHOLOGY_TEST_DESCRIPTION("PATH_TEST_DESC"),
    /**
     * The coding system (vocabulary) that we are using (eg. WinPath).
     */
    PATHOLOGY_CODING_SYSTEM("PATH_COD_SYS"),
    /**
     * The numeric value for a pathology result.
     */
    PATHOLOGY_NUMERIC_VALUE("PATH_NUM_VALUE"),
    /**
     * The units for a numerical pathology result.
     */
    PATHOLOGY_UNITS("PATH_NUM_UNITS"),
    /**
     * Pathology order fact type.
     */
    PATHOLOGY_ORDER("PATH_ORDER"),
    /**
     * Pathology order control ID.
     */
    PATHOLOGY_ORDER_CONTROL_ID("PATH_ORD_CTL_ID"),
    /**
     * Pathology order number (Epic order number).
     */
    PATHOLOGY_EPIC_ORDER_NUMBER("PATH_EPIC_NUM"),
    /**
     * Pathology order lab/specimen number, aka accession number.
     */
    PATHOLOGY_LAB_NUMBER("PATH_LAB_NUM"),
    /**
     * Pathology order lab/specimen number with extra appended character, aka OCS number.
     */
    PATHOLOGY_OCS_NUMBER("PATH_OCS_NUM");

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

    private static Map<String, AttributeKeyMap> values;

    /**
     * Read the Attributes back from a String matching its shortname.
     *
     * @param text String to read from
     * @return Matching AttributeKeyMap
     */
    public static AttributeKeyMap parseFromShortName(String text) {
        if (values == null) {
            values = new HashMap<>();
            for (AttributeKeyMap a: AttributeKeyMap.values()) {
                values.put(a.shortname.toUpperCase(), a);
            }
        }
        return values.get(text.toUpperCase());
    }
}
