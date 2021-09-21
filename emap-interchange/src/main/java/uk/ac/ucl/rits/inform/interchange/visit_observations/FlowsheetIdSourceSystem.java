package uk.ac.ucl.rits.inform.interchange.visit_observations;

/**
 * @author Jeremy Stein
 *
 */
public enum FlowsheetIdSourceSystem {
    /**
     * An ID from the HL7 interface (aka MPI ID).
     */
    EPIC("EPIC"),

    /**
     * An ID from Caboodle.
     */
    CABOODLE("CABOODLE");

    private String stringVal;

    FlowsheetIdSourceSystem(String stringVal) {
        this.stringVal = stringVal;
    }

    @Override
    public String toString() {
        return stringVal;
    }
}
