package uk.ac.ucl.rits.inform.interchange.visit_observations;

/**
 * @author Jeremy Stein
 *
 */
public enum InterfaceIdSourceSystem {
    /**
     * An ID from the HL7 interface (aka MPI ID).
     */
    EPIC("EPIC"),

    /**
     * An ID from Caboodle.
     */
    CABOODLE("CABOODLE");

    private String stringVal;

    InterfaceIdSourceSystem(String stringVal) {
        this.stringVal = stringVal;
    }

    @Override
    public String toString() {
        return stringVal;
    }
}
