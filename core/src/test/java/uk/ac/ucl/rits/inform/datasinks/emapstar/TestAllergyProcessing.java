package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PatientConditionController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.controllers.PatientSymptomController;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientConditionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConditionSymptomRepository;

import uk.ac.ucl.rits.inform.informdb.conditions.ConditionSymptom;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.PatientAllergy;
import uk.ac.ucl.rits.inform.interchange.PatientProblem;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test the processing of allergy interchange messages and population of the patient condition databases
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

    private static final String CONDITION_TYPE = "PATIENT_ALLERGY";
    private static final String FIRST_MRN = "8DcEwvqa8Q3";
    private static final String FIRST_ALLERGEN = "TRAMADOL";
    private static final String FIRST_UPDATED_TIME = "2019-06-08T10:32:05Z";
    private static final String FIRST_ADDED_TIME = "2019-06-08T10:31:05Z";


    @BeforeEach
    private void setUp() throws IOException {
        hooverUpdatedMessages = messageFactory.getPatientAllergies("updated_only.yaml");
        hooverQueryOrderingMessages =  messageFactory.getPatientAllergies("query_ordering_with_nulls.yaml");
        hl7Tramadol = messageFactory.getPatientAllergies("hl7/minimal_allergy.yaml").get(0);
    }


    private boolean aSingleConditionExists(){
        return getAllEntities(patientConditionRepository).size() == 1;
    }

    private PatientCondition firstPatientCondition(){
        return getAllEntities(patientConditionRepository).get(0);
    }


    /**
     * Given that no patient conditions exist
     * When a minimal patient allergy message arrives
     * Then a patient condition is added which is linked to an allergen
     */
    @Test
    void testMinimalMessageProcessing() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7Tramadol);

        assertTrue(aSingleConditionExists());
        assertEquals(1, getAllEntities(conditionTypeRepository).size());
        assertEquals(1, getAllMrns().size());

        PatientCondition condition = firstPatientCondition();
        assertEquals(FIRST_MRN, condition.getMrnId().getMrn());

        assertEquals(Instant.parse(FIRST_ADDED_TIME), condition.getAddedDateTime());
        assertEquals(LocalDate.parse("2019-05-07"), condition.getOnsetDate());
        assertNull(condition.getPriority());
        assertNull(condition.getComment());
        assertNull(condition.getResolutionDateTime());
    }

    /**
     * Given that no conditionTypes exist
     * When a minimal message is processed containing an allergy
     * Then a single conditionType is created
     */
    @Test
    void testConditionTypeCreated() throws EmapOperationMessageProcessingException{

        assertEquals(0, conditionTypeRepository.count());
        processSingleMessage(hl7Tramadol);

        ConditionType type = conditionTypeRepository.findByDataTypeAndName(
                CONDITION_TYPE, FIRST_ALLERGEN).orElseThrow();

        assertEquals(FIRST_ALLERGEN, type.getName());
        assertEquals(CONDITION_TYPE, type.getDataType());
        assertEquals("DRUG INGREDI", type.getSubType());

        assertEquals(Instant.parse(FIRST_UPDATED_TIME), type.getValidFrom());
        assertNotNull(type.getValidFrom());
        assertNull(type.getStandardisedCode());
        assertNull(type.getStandardisedVocabulary());
    }


    /**
     * Given that no patient conditions exist
     * When a patient allergy message arrives containing reactions
     * Then a patient condition appropriate for the allergy is added and reactions populated in the ConditionSymptom table
     */
    @Test
    void testAllergyMessageProcessingWithReactions() throws EmapOperationMessageProcessingException{

        processSingleMessage(hooverUpdatedMessages.get(0));
        assertTrue(aSingleConditionExists());

        PatientCondition condition = firstPatientCondition();
        assertEquals(FIRST_MRN, condition.getMrnId().getMrn());
        assertEquals(1, condition.getInternalId());
        assertEquals("NUTS", condition.getConditionTypeId().getName());
        assertEquals(CONDITION_TYPE, condition.getConditionTypeId().getDataType());
        assertEquals("Food", condition.getConditionTypeId().getSubType());
        assertEquals(Instant.parse(FIRST_ADDED_TIME), condition.getAddedDateTime());
        assertEquals(LocalDate.parse("2019-03-05"), condition.getOnsetDate());
        assertEquals("High", condition.getConditionTypeId().getSeverity());
        assertEquals("Active", condition.getStatus());

        List<ConditionSymptom> reactions = getAllEntities(conditionSymptomRepository);
        assertEquals(2, reactions.size());

        List<String> reactionNames = Arrays.asList(reactions.get(0).getName(), reactions.get(1).getName());
        assertTrue(reactionNames.contains("Anaphylaxis"));
        assertTrue(reactionNames.contains("Hives"));
    }

    /**
     * Given that no patient conditions exist
     * When two patient allergy message arrive containing reactions
     * Then a patient conditions and symptoms are added appropriate for the messages
     */
    @Test
    void testMultipleAllergyMessageProcessingWithReactions() throws EmapOperationMessageProcessingException{

        processSingleMessage(hooverUpdatedMessages.get(0));
        processSingleMessage(hooverUpdatedMessages.get(1));
        assertEquals(2, getAllEntities(patientConditionRepository).size());

        // Allergy diagnoses may not be associated with hospital visits
        assertEquals(0, getAllEntities(hospitalVisitRepository).size());

        // Get the second condition and check the data
        PatientCondition condition = getAllEntities(patientConditionRepository).get(1);
        assertEquals("suI83US", condition.getMrnId().getMrn());
        assertEquals("SEROTONIN REUPTAKE INHIBITORS (SSRIS)", condition.getConditionTypeId().getName());
        assertEquals(CONDITION_TYPE, condition.getConditionTypeId().getDataType());
        assertEquals("Drug Class", condition.getConditionTypeId().getSubType());
        assertEquals(Instant.parse("2019-06-08T02:05:49Z"), condition.getAddedDateTime());
        assertEquals(LocalDate.parse("2019-03-05"), condition.getOnsetDate());
        assertEquals("Medium", condition.getConditionTypeId().getSeverity());
        assertEquals("Active", condition.getStatus());

        // Hives should only be added once to the symptom repository
        List<ConditionSymptom> reactions = getAllEntities(conditionSymptomRepository);
        assertEquals(3, reactions.size());

        List<String> reactionNames = new ArrayList<>();
        for (ConditionSymptom reaction: reactions) {
            reactionNames.add(reaction.getName());
        }

        assertTrue(reactionNames.contains("Hives"));
    }


    // TODO: What is this testing?!
    @Test
    void testX() throws EmapOperationMessageProcessingException{

        /*
        - "@class": "uk.ac.ucl.rits.inform.interchange.PatientAllergy"
          sourceSystem: "clarity"
          sourceMessageId: "3"
          updatedDateTime: "2019-05-10T09:40:25Z"
          mrn: "Bq6PQt5xSa"
          allergenType: "DRUG INGREDI"
          allergenName: "TRAMADOL"
          epicAllergyId: 3
          allergyAdded: "2019-05-10T09:40:25Z"
          allergyOnset: "2019-05-07"
          severity: "High"
          status: "Active"
        - "@class": "uk.ac.ucl.rits.inform.interchange.PatientAllergy"
          sourceSystem: "clarity"
          sourceMessageId: "4"
          updatedDateTime: "2019-05-14T08:19:11Z"
          mrn: "xo9dzpR0Zx7J"
          visitNumber: "100000013"
          allergenType: "DRUG INGREDI"
          allergenName: "CO-TRIMOXAZOLE (TRIMETHOPRIM AND SULFAMETHOXAZOLE)"
          epicAllergyId: 4
          allergyAdded: "2019-05-14T08:19:11Z"
          severity: "Low"
          status: "Deleted"
        - "@class": "uk.ac.ucl.rits.inform.interchange.PatientAllergy"
          sourceSystem: "clarity"
          sourceMessageId: "1"
          updatedDateTime: "2019-06-09T01:00:05Z"
          mrn: "8DcEwvqa8Q3"
          allergenType: "Food"
          allergenName: "NUTS"
          epicAllergyId: 1
          allergyAdded: "2019-06-08T10:31:05Z"
          allergyOnset: "2019-03-05"
          severity: "High"
          reactions:
            - "Anaphylaxis"
            - "Hives"
          status: "Active"
        - "@class": "uk.ac.ucl.rits.inform.interchange.PatientAllergy"
          sourceSystem: "clarity"
          sourceMessageId: "2"
          updatedDateTime: "2019-06-09T01:00:05Z"
          mrn: "suI83US"
          allergenType: "Drug Class"
          allergenName: "SEROTONIN REUPTAKE INHIBITORS (SSRIS)"
          epicAllergyId:  2
          allergyAdded: "2019-06-08T02:05:49Z"
          allergyOnset: "2019-03-05"
          severity: "Medium"
          reactions:
            - "Hives"
          status: "Active"
                 */

        for (PatientAllergy msg: hooverQueryOrderingMessages){
            processSingleMessage(msg);
        }

        assertEquals(4, getAllEntities(patientConditionRepository).size());

    }

    /**
     * Given that a patient condition exists
     * When a newer new message arrives that concerns the same patient
     * Then the condition is updated with the new information
     */
    @Test
    void testUpdateCommentOnNewHl7Message() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7Tramadol);
        assertTrue(aSingleConditionExists());
        assertNull(getAllEntities(patientConditionRepository).get(0).getComment());

        hl7Tramadol.setComment(InterchangeValue.buildFromHl7("test"));
        hl7Tramadol.setUpdatedDateTime(Instant.parse(FIRST_UPDATED_TIME).plus(1, ChronoUnit.SECONDS));

        processSingleMessage(hl7Tramadol);
        assertEquals("test", firstPatientCondition().getComment());
    }

    /**
     * Given that a patient condition exists
     * When an older new message arrives that concerns the same patient
     * Then the condition is not updated with the new information
     */
    @Test
    void testUpdateCommentOnOldHl7Message() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7Tramadol);
        assertTrue(aSingleConditionExists());
        assertNull(firstPatientCondition().getStatus());

        hl7Tramadol.setStatus(InterchangeValue.buildFromHl7("ACTIVE"));
        hl7Tramadol.setUpdatedDateTime(Instant.parse(FIRST_UPDATED_TIME).minus(1, ChronoUnit.SECONDS));

        processSingleMessage(hl7Tramadol);
        assertNull(firstPatientCondition().getStatus());
    }

}
