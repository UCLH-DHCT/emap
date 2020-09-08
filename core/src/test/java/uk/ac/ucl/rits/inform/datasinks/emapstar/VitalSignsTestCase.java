/**
 *
 */
package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.OldAttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Set up the patient to add vital signs to. To make it more interesting, give
 * them two MRNs which have been merged.
 *
 * @author Jeremy Stein
 */
public class VitalSignsTestCase extends MessageStreamBaseCase {

    public VitalSignsTestCase() {}

    private String mrn1 = "ALICE";
    private String csn1 = "bob";

    private String mrn2 = "CAROL";
    private String csn2 = "dave";

    @Transactional
    public void setMrn1() {
        this.mrn = mrn1;
        this.csn = csn1;
    }

    @Transactional
    public void setMrn2() {
        this.mrn = mrn2;
        this.csn = csn2;
    }

    @BeforeEach
    @Transactional
    public void queueMerge() {

        this.setMrn1();

        this.queueAdmit(false, "I");
        this.queueDischarge();

        this.setMrn2();
        this.queueAdmit();
        this.queueMerge(this.mrn1, this.mrn2);

    }

    @Test
    @Transactional
    public void testVitalsNormal() throws EmapOperationMessageProcessingException {
        // add to older encounter, referring to its original but now retired MRN
        this.setMrn1();
        this.vitalReading = 92.;
        this.queueVital();

        // Now use the new mrn
        this.setMrn2();
        this.vitalReading = 93.;
        this.queueVital();

        this.processRest();

        _testHeartRatePresent(this.mrn1, this.csn1, 92, this.vitalTime.get(0));
        _testHeartRatePresent(this.mrn2, this.csn2, 93, this.vitalTime.get(1));
    }

    @Test
    @Transactional
    public void testVitalsTricky() throws EmapOperationMessageProcessingException {
        // add to older encounter, but refer to the surviving MRN that subsumed its MRN
        this.mrn = mrn1;
        this.csn = csn2;
        this.vitalReading = 94.;
        this.queueVital();

        // add to newer encounter, but refer to the non-surviving MRN
        this.mrn = mrn2;
        this.csn = csn1;
        this.vitalReading = 95.;
        this.queueVital();

        this.processRest();

        _testHeartRatePresent(this.mrn2, this.csn2, 94, this.vitalTime.get(0));
        _testHeartRatePresent(this.mrn1, this.csn1, 95, this.vitalTime.get(1));
    }

    @Transactional
    public void _testHeartRatePresent(String mrnStr, String encounterStr, int expectedHeartRate, Instant expectedTime) {
        List<PatientFact> vitalSigns =
                patientFactRepo.findAllByEncounterAndFactType(encounterStr, OldAttributeKeyMap.VITAL_SIGN);
        assertEquals(1, vitalSigns.size());
        PatientFact vit = vitalSigns.get(0);
        Encounter encounter = vit.getEncounter();
        assertEquals(mrnStr, encounter.getMrns().get(0).getMrn().getMrn());
        assertEquals(new Double(expectedHeartRate),
                vit.getPropertyByAttribute(OldAttributeKeyMap.VITAL_SIGNS_NUMERIC_VALUE).get(0).getValueAsReal());
        assertEquals("HEART_RATE", vit.getPropertyByAttribute(OldAttributeKeyMap.VITAL_SIGNS_OBSERVATION_IDENTIFIER).get(0)
                .getValueAsString());
        assertTrue(vit.getPropertyByAttribute(OldAttributeKeyMap.VITAL_SIGNS_STRING_VALUE).isEmpty());
        assertEquals("/min", vit.getPropertyByAttribute(OldAttributeKeyMap.VITAL_SIGNS_UNIT).get(0).getValueAsString());
        assertEquals(expectedTime,
                vit.getPropertyByAttribute(OldAttributeKeyMap.VITAL_SIGNS_OBSERVATION_TIME).get(0).getValueAsDatetime());
    }
}
