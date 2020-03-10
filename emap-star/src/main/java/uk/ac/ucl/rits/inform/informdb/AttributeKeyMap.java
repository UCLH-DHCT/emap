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
     * The patient class (eg. inpatient, outpatient, emergency, etc.)
     * as defined by the source system, represented as a string.
     */
    PATIENT_CLASS("PATIENT_CLASS"),
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
     * Reference range for the result.
     */
    PATHOLOGY_REFERENCE_RANGE("PATH_REF_RANGE"),
    /**
     * When the result was last updated.
     */
    PATHOLOGY_RESULT_TIME("PATH_RES_TIME"),
    /**
     * The status of the result (final, etc.).
     */
    PATHOLOGY_RESULT_STATUS("PATH_RES_STS"),
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
    PATHOLOGY_OCS_NUMBER("PATH_OCS_NUM"),
    /**
     * Code that identifies the lab department this order/result came from.
     */
    PATHOLOGY_LAB_DEPARTMENT_CODE("PATH_LAB_DEPT"),
    /**
     * When the sample was collected (observation time).
     */
    PATHOLOGY_COLLECTION_TIME("PATH_COLL_TIME"),
    /**
     * Local code of the isolate (microbiology).
     */
    PATHOLOGY_ISOLATE_CODE("PATH_ISOL_CODE"),
    /**
     * When the order was originally made.
     */
    PATHOLOGY_ORDER_TIME("PATH_ORDER_TIME"),
    /**
     * The type of the patient that the order relates to (inpatient, outpatient).
     */
    PATHOLOGY_ORDER_PATIENT_TYPE("PATH_ORDER_TYPE"),
    /**
     * The pathology order status from WinPath.
     */
    PATHOLOGY_ORDER_ORDER_STATUS("PATH_ORDER_STS"),
    /**
     * The pathology result status from WinPath.
     */
    PATHOLOGY_ORDER_RESULT_STATUS("PATH_RESULT_STS"),
    /**
     * Time of last change of status for the order.
     * If this is for results, PATHOLOGY_RESULT_TIME should be used instead.
     */
    PATHOLOGY_STATUS_CHANGE_TIME("PATH_STS_TIME"),
    /**
     * Vital sign fact type.
     */
    VITAL_SIGN("VIT_SIGN"),
    /**
     * The local identifier (code) that tells us which vital sign it is.
     */
    VITAL_SIGNS_OBSERVATION_IDENTIFIER("VIT_OBS_ID"),
    /**
     * The coding system being used for VITAL_SIGNS_OBSERVATION_IDENTIFIER (eg. WinPath)
     */
    VITAL_SIGNS_OBSERVATION_CODING_SYSTEM("VIT_OBS_COD_SYS"),
    /**
     * The vital sign value if it's a string.
     */
    VITAL_SIGNS_STRING_VALUE("VIT_STR_VALUE"),
    /**
     * The vital sign value if it's a number.
     */
    VITAL_SIGNS_NUMERIC_VALUE("VIT_NUM_VALUE"),
    /**
     * The unit of the vital sign value.
     */
    VITAL_SIGNS_UNIT("VIT_UNIT"),
    /**
     * The time the vital sign was observed.
     */
    VITAL_SIGNS_OBSERVATION_TIME("VIT_OBS_TIME");

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
