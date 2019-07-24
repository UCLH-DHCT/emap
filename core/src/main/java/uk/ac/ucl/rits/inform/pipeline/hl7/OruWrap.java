package uk.ac.ucl.rits.inform.pipeline.hl7;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v27.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v27.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v27.group.ORU_R01_PATIENT;
import ca.uhn.hl7v2.model.v27.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v27.message.ORU_R01;
import ca.uhn.hl7v2.model.v27.segment.MSH;
import ca.uhn.hl7v2.model.v27.segment.OBR;
import ca.uhn.hl7v2.model.v27.segment.OBX;
import ca.uhn.hl7v2.model.v27.segment.PID;
import ca.uhn.hl7v2.model.v27.segment.PV1;
import uk.ac.ucl.rits.inform.pipeline.exceptions.SkipPathologyResult;

/**
 * Parse and make sense of a pathology result.
 * @author Jeremy Stein
 *
 */
public class OruWrap implements PV1Wrap, PIDWrap, MSHWrap {
    private static final Logger logger = LoggerFactory.getLogger(OruWrap.class);
    private MSH msh;
    private PV1 pv1;
    private PID pid;
    private List<PathologyResult> allPath = new ArrayList<>();

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

    /*
     * ORU_R01_PATIENT_RESULT repeating
     *      ORU_R01_PATIENT optional
     *          PID (Patient Identification)
     *          PRT (Participation Information) optional repeating
     *          PD1 (Patient Additional Demographic) optional
     *          NTE (Notes and Comments) optional repeating
     *          NK1 (Next of Kin / Associated Parties) optional repeating
     *          ORU_R01_PATIENT_OBSERVATION (a Group object) optional repeating
     *              OBX (Observation/Result)
     *              PRT (Participation Information) optional repeating
     *          ORU_R01_VISIT (a Group object) optional
     *              PV1 (Patient Visit)
     *              PV2 (Patient Visit - Additional Information) optional
     *              PRT (Participation Information) optional repeating
     *      ORU_R01_ORDER_OBSERVATION repeating
     *          ORC (Common Order) optional
     *          OBR (Observation Request)
     *          NTE (Notes and Comments) optional repeating
     *          PRT (Participation Information) optional repeating
     *          ORU_R01_TIMING_QTY (a Group object) optional repeating
     *          CTD (Contact Data) optional
     *          ORU_R01_OBSERVATION (a Group object) optional repeating
     *              OBX (Observation/Result)
     *              PRT (Participation Information) optional repeating
     *              NTE (Notes and Comments) optional repeating
     *          FT1 (Financial Transaction) optional repeating
     *          CTI (Clinical Trial Identification) optional repeating
     *          ORU_R01_SPECIMEN (a Group object) optional repeating
     *              SPM (Specimen)
     *              ORU_R01_SPECIMEN_OBSERVATION (a Group object) optional repeating
     *                  OBX (Observation/Result)
     *                  PRT (Participation Information) optional repeating
     */

    /**
     * Dive into the segments that constitute an ORU message.
     * Turn into a flatter format.
     * @param oruMsg the HL7 message
     * @throws HL7Exception if HAPI does
     */
    public OruWrap(Message oruMsg) throws HL7Exception {
        ORU_R01 oruR01 = (ORU_R01) oruMsg;
        msh = (MSH) oruMsg.get("MSH");

        List<ORU_R01_PATIENT_RESULT> allPatientResults = oruR01.getPATIENT_RESULTAll();
        if (allPatientResults.size() != 1) {
            throw new RuntimeException("not handling this yet");
        }
        for (ORU_R01_PATIENT_RESULT res : allPatientResults) {
            ORU_R01_PATIENT patient = res.getPATIENT();
            pid = patient.getPID();
            pv1 = patient.getVISIT().getPV1();
            List<ORU_R01_ORDER_OBSERVATION> orderObservations = res.getORDER_OBSERVATIONAll();
            for (ORU_R01_ORDER_OBSERVATION oo : orderObservations) {
                OBR obr = oo.getOBR();
                List<ORU_R01_OBSERVATION> observationAll = oo.getOBSERVATIONAll();
                for (ORU_R01_OBSERVATION obs : observationAll) {
                    OBX obx = obs.getOBX();
                    try {
                        PathologyResult pathologyResult = new PathologyResult(obx, obr);
                        allPath.add(pathologyResult);
                    } catch (SkipPathologyResult sk) {
                    }
                }
            }
        }
    }

    /**
     * @return all pathology results in this message
     */
    public List<PathologyResult> getAllPathologyResults() {
        return allPath;
    }

}
