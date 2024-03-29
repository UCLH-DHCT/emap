package uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.segment;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractSegment;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import uk.ac.ucl.rits.inform.datasources.ids.hl7.custom.v26.field.Infection;

/**
 * Patient infections segment from EPIC interface.
 * Can have multiple infections in a segment
 * Based on documentation:
 * https://hapifhir.github.io/hapi-hl7v2/xref/ca/uhn/hl7v2/examples/custommodel/v25/segment/ZPI.html
 */
public class ZIF extends AbstractSegment {

    private static final int MAX_REPS = 0;
    private static final int MAX_LENGTH = 250;

    /**
     * Create ZIF segment using HAPI conventions.
     * @param parent parent group for the segment
     * @param factory to look up classes for concrete implementation of parts of a message
     */
    public ZIF(Group parent, ModelClassFactory factory) {
        super(parent, factory);
        // By convention, an init() method is created which adds the specific fields to this segment class
        init(factory);
    }

    private void init(ModelClassFactory factory) {
        try {
            // Add in infections
            add(Infection.class, true, MAX_REPS, MAX_LENGTH, new Object[]{getMessage()}, "infections");
        } catch (HL7Exception e) {
            log.error("Unexpected error creating ZIF - this is probably a bug in the source code generator.", e);
        }
    }

    /**
     * This method must be overridden. The easiest way is just to return null.
     */
    @Override
    protected Type createNewTypeWithoutReflection(int field) {
        return null;
    }

    /**
     * Create an accessor for each field.
     * @param rep index for field
     * @return Infection
     */
    public Infection getInfection(int rep) {
        return getTypedField(1, rep);
    }

    public int getInfectionReps() {
        return this.getReps(1);
    }


}
