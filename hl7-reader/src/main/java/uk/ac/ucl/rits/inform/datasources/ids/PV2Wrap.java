package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.segment.PV2;

/**
 * Wrapper around the HAPI parser's PV2 segment object, to make it easier to use.
 * Reference page: https://hapifhir.github.io/hapi-hl7v2/v27/apidocs/ca/uhn/hl7v2/model/v27/segment/PV2.html
 */
interface PV2Wrap {
    /**
     * @return the PV2 object
     */
    PV2 getPV2();

    /**
     * @return whether the PV2 segment exists
     */
    default boolean pv2SegmentExists() {
        return getPV2() != null;
    }

    /**
     * @return PV2-38.1 Mode of arrival Core
     * @throws HL7Exception if HAPI does
     */
    default String getModeOfArrivalCode() throws HL7Exception {
        return getPV2().getModeOfArrivalCode().getCwe1_Identifier().getValueOrEmpty();
    }

}
