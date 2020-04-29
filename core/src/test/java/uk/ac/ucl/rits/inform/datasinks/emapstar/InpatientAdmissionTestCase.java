package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

public class InpatientAdmissionTestCase extends MessageStreamBaseCase {

    public InpatientAdmissionTestCase() {
    }

    @Test
    @Transactional
    public void basicAdmit() throws EmapOperationMessageProcessingException {
        this.queueAdmit();
        this.processRest();
        this.testInpAdmission();
    }

    /**
     * A common ED message sequence is A04, A08, A01. All with patient class E I assume?
     * Is the A01 optional, if eg a patient was turned away at an early stage?
     */
    @Transactional
    public void testInpAdmission() {
        Encounter enc = encounterRepo.findEncounterByEncounter(this.csn);
        Map<AttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsGroupByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsGroupByType.get(AttributeKeyMap.BED_VISIT);
        List<PatientFact> outpVisits = factsGroupByType.get(AttributeKeyMap.OUTPATIENT_VISIT);
        assertEquals(1, hospVisits.size());
        PatientFact onlyHospVisit = hospVisits.get(0);
        assertNull(outpVisits);
        assertEquals(1, bedVisits.size());
        assertIsParentOfChildren(onlyHospVisit, bedVisits);

        assertEquals(this.patientClass,
                bedVisits.get(0).getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).get(0).getValueAsString());

        assertEquals(this.patientClass,
                hospVisits.get(0).getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).get(0).getValueAsString());
    }
}
