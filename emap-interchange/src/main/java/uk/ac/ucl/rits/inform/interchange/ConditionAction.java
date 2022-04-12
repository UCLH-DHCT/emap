package uk.ac.ucl.rits.inform.interchange;

/**
 * An action to take when processing a patient condition message, e.g. problem or infection. See:
 * https://hl7-definition.caristix.com/v2/HL7v2.7/Tables/0287
 */
public enum ConditionAction {

    /**
     * Add.
     */
    AD,
    /**
     * Update.
     */
    UP,
    /**
     * Delete.
     */
    DE
}
