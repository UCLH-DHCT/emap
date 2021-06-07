package uk.ac.ucl.rits.inform.datasources.ids.hl7.parser;

import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.model.v26.segment.PV2;

/**
 * Group together some common functionality that gets patient and visit info.
 * Each message type can have a different way of finding these segments,
 * hence why it's up to the caller to find them.
 * @author Jeremy Stein
 * @author Stef Piatek
 */
public class PatientInfoHl7 implements PV1Wrap, PV2Wrap, PIDWrap, MSHWrap {
    private final MSH msh;
    private final PV1 pv1;
    private final PV2 pv2;
    private final PID pid;

    @Override
    public PV1 getPV1() {
        return pv1;
    }

    @Override
    public MSH getMSH() {
        return msh;
    }

    @Override
    public PID getPID() {
        return pid;
    }

    @Override
    public PV2 getPV2() {
        return pv2;
    }

    /**
     * Build the parser object from some basic segments.
     * @param msh the MSH segment from the message
     * @param pid the PID segment from the message
     * @param pv1 the PV1 segment from the message
     */
    public PatientInfoHl7(MSH msh, PID pid, PV1 pv1) {
        this.msh = msh;
        this.pv1 = pv1;
        this.pid = pid;
        pv2 = null;
    }

    /**
     * Build the parser object from all patient information segments used in ADT.
     * @param msh the MSH segment from the message
     * @param pid the PID segment from the message
     * @param pv1 the PV1 segment from the message
     * @param pv2 the PV2 segment from the message
     */
    public PatientInfoHl7(MSH msh, PID pid, PV1 pv1, PV2 pv2) {
        this.msh = msh;
        this.pid = pid;
        this.pv1 = pv1;
        this.pv2 = pv2;
    }

}
