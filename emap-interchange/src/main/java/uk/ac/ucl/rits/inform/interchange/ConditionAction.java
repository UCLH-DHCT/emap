package uk.ac.ucl.rits.inform.interchange;

/**
 * An action to take when processing a patient condition message, e.g. problem or infection. See:
 * https://hl7-definition.caristix.com/v2/HL7v2.7/Tables/0287
 */
public enum ConditionAction {

    /**
     * Add.
     */
    ADD("AD"),
    /**
     * Update.
     */
    // How much do we care about the difference, we could do this just a save and delete?
    // If so, we could use the result status and have a method on that to build from condition action
    UPDATE("UP"),
    /**
     * Delete.
     */
    DELETE("DE");

    private final String hl7Value;

    ConditionAction(String hl7Value) {
        this.hl7Value = hl7Value;
    }

    /**
     * Find ConditionAction by name.
     * @param hl7Value status name from hl7
     * @return ConditionAction
     * @throws IllegalArgumentException if an unknown name is encountered
     */
    public static ConditionAction findByHl7Value(String hl7Value) {
        for (ConditionAction conditionAction : values()) {
            if (conditionAction.hl7Value.equals(hl7Value)) {
                return conditionAction;
            }
        }
        throw new IllegalArgumentException(String.format("ConditionAction '%s' is not a known type", hl7Value));
    }
}
