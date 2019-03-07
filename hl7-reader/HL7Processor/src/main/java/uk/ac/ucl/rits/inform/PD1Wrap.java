// PD1Wrap.java

package uk.ac.ucl.rits.inform;

import ca.uhn.hl7v2.HL7Exception;

import ca.uhn.hl7v2.model.v27.segment.PD1;

/**
 * class PD1Wrap
 * 
 * Wrapper around the HAPI parser's PD1 segment object, to make it easier to use.
 * 
 * Reference: https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/PD1.html
 */

 public class PD1Wrap {

    private PD1 _pd1;

    /**
     * Constructor
     * 
     * @param myPD1 PD1 segment, obtained by parsing the message to which this segment relates (msg.getPD1())
     */
    public PD1Wrap(PD1 myPD1) {
        _pd1 = myPD1;
    }


    /**
     * 
     * @return PD1-6 Disability. Not in Epic spec.
     * @throws HL7Exception
     */
    public String getDisability() throws HL7Exception {
        return _pd1.getHandicap().getText().toString();
    }


 }



