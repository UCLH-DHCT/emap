package uk.ac.ucl.rits.inform.pipeline.hl7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v27.group.ADT_A39_PATIENT;
import ca.uhn.hl7v2.model.v27.message.ADT_A39;
import ca.uhn.hl7v2.model.v27.segment.EVN;
import ca.uhn.hl7v2.model.v27.segment.MRG;
import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.model.v27.segment.PD1;
import ca.uhn.hl7v2.model.v27.segment.PID;
import ca.uhn.hl7v2.model.v27.segment.PV1;

/**
 * Wrapper for an ADT message so we can find what we need more easily.
 *
 * @author Jeremy Stein
 */
public class AdtWrap implements PV1Wrap, EVNWrap, MSHWrap, PIDWrap {
    private static final Logger logger = LoggerFactory.getLogger(AdtWrap.class);

    private MSH msh;
    private PV1 pv1;
    private PID pid;
    private PD1Wrap pd1wrap;
    private EVN evn;
    private MRG mrg;

    @Override
    public PV1 getPV1() {
        return pv1;
    }

    @Override
    public EVN getEVN() {
        return evn;
    }

    @Override
    public MSH getMSH() {
        return msh;
    }

    @Override
    public PID getPID() {
        return pid;
    }

    /**
     * Make a random data generating version of the object.
     * Override this.
     */
    protected AdtWrap() {
    }

    /**
     * Populate the data from an HL7 message.
     *
     * @param adtMsg the passed in HL7 message
     * @throws HL7Exception if HAPI does
     */
    public AdtWrap(Message adtMsg) throws HL7Exception {
        msh = (MSH) adtMsg.get("MSH");

        try {
            pv1 = (PV1) adtMsg.get("PV1");
        } catch (HL7Exception e) {
            // sections are allowed not to exist
        }

        // I want the "MRG" segment for A40 messages, is this really
        // the best way to get it? Why do we have to get the PID segment in
        // a different way for an A39/A40 message?
        if (adtMsg instanceof ADT_A39) {
            ADT_A39_PATIENT a39Patient = (ADT_A39_PATIENT) adtMsg.get("PATIENT");
            mrg = a39Patient.getMRG();
            pid = a39Patient.getPID();
        } else {
            try {
                pid = (PID) adtMsg.get("PID");
            } catch (HL7Exception e) {
            }
        }
        try {
            pd1wrap = new PD1Wrap((PD1) adtMsg.get("PD1"));
        } catch (HL7Exception e) {
        }
        try {
            evn = (EVN) adtMsg.get("EVN");
        } catch (HL7Exception e) {
            // EVN is allowed not to exist
        }
    }

    /**
     * Print some basic debug information about the HL7 message.
     *
     * @throws HL7Exception if HAPI does
     */
    public void debug() throws HL7Exception {
        logger.debug("\n************** MSH segment **************************");
        logger.debug("sending application = " + getSendingApplication());
        logger.debug("sending facility = " + getSendingFacility());
        logger.debug("messageTimestamp = " + getMessageTimestamp());
        logger.debug("message type = " + getMessageType()); // MSH-9.1 Message
        logger.debug("trigger event = " + getTriggerEvent());
        logger.debug("current bed = " + getCurrentBed());
        logger.debug("patient name = " + getPatientFullName());
        logger.debug("patient MRN = " + getMrn());
        logger.debug("admission time = " + getAdmissionDateTime());
    }

    /**
     * @return the non-surviving patient ID from a merge message
     */
    public String getMergedPatientId() {
        return mrg.getMrg1_PriorPatientIdentifierList(0).getIDNumber().toString();
    }

}
