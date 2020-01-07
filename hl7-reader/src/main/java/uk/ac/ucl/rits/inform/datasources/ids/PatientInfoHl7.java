package uk.ac.ucl.rits.inform.datasources.ids;

import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.PID;
import ca.uhn.hl7v2.model.v26.segment.PV1;

/**
 * Group together some common functionality that gets patient and visit info.
 * Each message type can have a different way of finding these segments,
 * hence why it's up to the caller to find them.
 *
 * @author Jeremy Stein
 */
public class PatientInfoHl7 implements PV1Wrap, PIDWrap, MSHWrap {
    private MSH msh;
    private PV1 pv1;
    private PID pid;

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

    /**
     * Build the parser object from some basic segments.
     * @param msh the MSH segment from the message
     * @param pid the PID segment from the message
     * @param pv1 the PV1 segment from the message
     */
    public PatientInfoHl7(MSH msh, PID pid, PV1 pv1) {
        this.msh = msh;
        this.pid = pid;
        this.pv1 = pv1;
    }


}
