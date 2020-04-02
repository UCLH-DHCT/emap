package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.CheckedInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.Encounter;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

public class InpatientAdmissionTestCase extends MessageStreamTestCase {

    public InpatientAdmissionTestCase() {
    }

    @Before
    public void setup() throws EmapOperationMessageProcessingException {
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.ADMIT_PATIENT);
            setAdmissionDateTime(Instant.now());
            setEventOccurredDateTime(Instant.now());
            setMrn("1234ABCD");
            setVisitNumber("1234567890");
            setPatientClass("I");
            setPatientFullName("Fred Bloggs");
            setFullLocationString("T42^BADGERS^WISCONSIN");
        }});
    }

    /**
     * A common ED message sequence is A04, A08, A01. All with patient class E I assume?
     * Is the A01 optional, if eg a patient was turned away at an early stage?
     */
    @Test
    @Transactional
    public void testInpAdmission() {
        Encounter enc = encounterRepo.findEncounterByEncounter("1234567890");
        Map<AttributeKeyMap, List<PatientFact>> factsGroupByType = enc.getFactsGroupByType();
        List<PatientFact> hospVisits = factsGroupByType.get(AttributeKeyMap.HOSPITAL_VISIT);
        List<PatientFact> bedVisits = factsGroupByType.get(AttributeKeyMap.BED_VISIT);
        List<PatientFact> outpVisits = factsGroupByType.get(AttributeKeyMap.OUTPATIENT_VISIT);
        assertEquals(1, hospVisits.size());
        PatientFact onlyHospVisit = hospVisits.get(0);
        assertNull(outpVisits);
        assertEquals(1, bedVisits.size());
        assertIsParentOfChildren(onlyHospVisit, bedVisits);

        assertEquals("I",
                bedVisits.get(0).getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).get(0).getValueAsString());

        assertEquals("I",
                hospVisits.get(0).getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).get(0).getValueAsString());
    }
}
