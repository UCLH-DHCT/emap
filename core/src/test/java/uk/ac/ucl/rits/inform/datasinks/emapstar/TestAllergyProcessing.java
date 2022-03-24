package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PatientSymptomController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.*;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientAllergy;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * T
 * @author Tom Young
 */
public class TestAllergyProcessing extends MessageProcessingBase {
    @Autowired
    PatientConditionRepository patientConditionRepository;
    @Autowired
    ConditionTypeRepository conditionTypeRepository;
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    ConditionSymptomRepository conditionSymptomRepository;

    private List<PatientAllergy> hooverQueryOrderingMessages;
    private List<PatientAllergy> hooverUpdatedMessages;
    private PatientAllergy hl7Tramadol;

    private static final String SAMPLE_COMMENT = "a comment";

    @BeforeEach
    private void setUp() throws IOException {
        hooverUpdatedMessages = messageFactory.getPatientAllergies("updated_only.yaml");
        hooverQueryOrderingMessages =  messageFactory.getPatientAllergies("query_ordering_with_nulls.yaml");
        hl7Tramadol = messageFactory.getPatientAllergies("hl7/minimal_allergy.yaml").get(0);
    }


    /**
     * Given that no patient conditions exist
     * When a minimal patient allergy message arrives
     * Then a patient condition is added which is linked to a symptom (reaction) to the allergen
     */
    @Test
    void testCreateProblemOutpatient() throws EmapOperationMessageProcessingException {

        /*
          sourceSystem: "EPIC"
          sourceMessageId: "0000000042"
          updatedDateTime: "2019-06-08T10:32:05Z"
          mrn: "8DcEwvqa8Q3"
          allergenType: "DRUG INGREDI"
          allergenName: "TRAMADOL"
          allergyAdded: "2019-06-08T10:31:05Z"
          allergyOnset: "2019-05-07"
         */

        processSingleMessage(hl7Tramadol);

        List<PatientCondition> conditions = getAllEntities(patientConditionRepository);
        assertEquals(1, conditions.size());

        PatientCondition conditon = conditions.get(0);
        assertEquals("8DcEwvqa8Q3", conditon.getMrnId().getMrn());
        assertEquals("TRAMADOL", conditon.getConditionTypeId().getName());
        assertEquals("DRUG INGREDI", conditon.getConditionTypeId().getSubType());
        assertEquals(Instant.parse("2019-06-08T10:31:05Z"), conditon.getAddedDateTime());
        assertEquals(LocalDate.parse("2019-05-07"), conditon.getOnsetDate());
    }

}
