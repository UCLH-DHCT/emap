package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TestPatientAllergyProcessing extends MessageProcessingBase {
    @Autowired
    PatientConditionRepository patientConditionRepository;
    @Autowired
    ConditionTypeRepository conditionTypeRepository;
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;
    @Autowired
    ConditionSymptomRepository conditionSymptomRepository;

    private List<PatientAllergy> hooverMessages;
    private PatientAllergy hl7Tramadol;

    private static final String CONDITION_TYPE = "PATIENT_ALLERGY";
    private static final String FIRST_MRN = "8DcEwvqa8Q3";
    private static final String FIRST_ALLERGEN = "TRAMADOL";
    private static final String FIRST_ALLERGEN_SUBTYPE = "DRUG INGREDI";
    private static final String FIRST_UPDATED_TIME = "2019-06-08T10:32:05Z";
    private static final String FIRST_ADDED_TIME = "2019-06-08T10:31:05Z";
    private static final String FIRST_ONSET_DATE = "2019-05-07";
    private static final Integer NUM_TRAMADOL_REACTIONS = 0;
    private static final String ACTIVE = "Active";

    private static final String SECOND_ALLERGEN = "NUTS";
    private static final String SECOND_ALLERGEN_SUBTYPE = "Food";
    private static final String SECOND_ONSET_DATE = "2019-05-07";
    private static final String SECOND_ALLERGY_SEVERITY = "High";
    private static final String[] SECOND_ALLERGY_REACTIONS = {"Anaphylaxis", "Hives"};

    private static final String THIRD_MRN = "suI83US";
    private static final String THIRD_ALLERGEN = "SEROTONIN REUPTAKE INHIBITORS (SSRIS)";
    private static final String THIRD_ALLERGEN_SUBTYPE = "Drug Class";
    private static final String THIRD_ONSET_DATE = "2019-03-05";
    private static final String THIRD_SEVERITY = "Medium";


    @BeforeEach
    private void setUp() throws IOException {
        hooverMessages = messageFactory.getPatientAllergies("updated_only.yaml");
        hl7Tramadol = messageFactory.getPatientAllergies("hl7/minimal_allergy.yaml").get(0);
    }

    private boolean aSingleConditionExists(){
        return getAllEntities(patientConditionRepository).size() == 1;
    }

    private PatientCondition firstPatientCondition(){
        return getAllEntities(patientConditionRepository).get(0);
    }

    private boolean hasNoPriorityCommentOrResolutionTime(PatientCondition condition){
        return condition.getPriority() == null && condition.getComment() == null && condition.getResolutionDatetime() == null;
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
        assertEquals(NUM_TRAMADOL_REACTIONS, getAllEntities(conditionSymptomRepository).size());
        assertEquals(Instant.parse(FIRST_ADDED_TIME), condition.getAddedDatetime());
        assertEquals(LocalDate.parse(FIRST_ONSET_DATE), condition.getOnsetDate());
        assertTrue(hasNoPriorityCommentOrResolutionTime(condition));
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

        ConditionType type = conditionTypeRepository.findByDataTypeAndName(CONDITION_TYPE, FIRST_ALLERGEN).orElseThrow();

        assertEquals(FIRST_ALLERGEN, type.getName());
        assertEquals(CONDITION_TYPE, type.getDataType());
        assertEquals(FIRST_ALLERGEN_SUBTYPE, type.getSubType());

        assertEquals(Instant.parse(FIRST_UPDATED_TIME), type.getValidFrom());
        assertNotNull(type.getValidFrom());
        assertNull(type.getStandardisedCode());
        assertNull(type.getStandardisedVocabulary());
    }

    /**
     * Given that no patient allergy conditions exist
     * When a patient allergy message arrives containing reactions
     * Then a patient condition appropriate for the allergy is added and reactions populated in the ConditionSymptom table
     */
    @Test
    void testAllergyMessageProcessingWithReactions() throws EmapOperationMessageProcessingException{

        processSingleMessage(hooverMessages.get(0));
        assertTrue(aSingleConditionExists());

        PatientCondition condition = firstPatientCondition();
        assertEquals(FIRST_MRN, condition.getMrnId().getMrn());
        assertEquals(1, condition.getInternalId());
        assertEquals(SECOND_ALLERGEN, condition.getConditionTypeId().getName());
        assertEquals(CONDITION_TYPE, condition.getConditionTypeId().getDataType());
        assertEquals(SECOND_ALLERGEN_SUBTYPE, condition.getConditionTypeId().getSubType());
        assertEquals(Instant.parse(FIRST_ADDED_TIME), condition.getAddedDatetime());
        assertEquals(LocalDate.parse(SECOND_ONSET_DATE), condition.getOnsetDate());
        assertEquals(SECOND_ALLERGY_SEVERITY, condition.getConditionTypeId().getSeverity());
        assertEquals(ACTIVE, condition.getStatus());
        assertTrue(hasNoPriorityCommentOrResolutionTime(condition));

        List<ConditionSymptom> reactions = getAllEntities(conditionSymptomRepository);
        assertEquals(2, reactions.size());

        List<String> reactionNames = Arrays.asList(reactions.get(0).getName(), reactions.get(1).getName());

        for (String reactionName : SECOND_ALLERGY_REACTIONS){
            assertTrue(reactionNames.contains(reactionName));

        }
    }

    /**
     * Given that no patient allergy conditions exist
     * When the same allergy message is processed twice
     * Then a single copy of the condition and reactions are present
     */
    @Test
    void testMultipleProcessingSameMessage() throws EmapOperationMessageProcessingException {

        processSingleMessage(hooverMessages.get(0));
        processSingleMessage(hooverMessages.get(0));

        assertTrue(aSingleConditionExists());
        assertEquals(1, getAllEntities(conditionTypeRepository).size());
        assertEquals(2, getAllEntities(conditionSymptomRepository).size());
    }

    /**
     * Given that no patient allergy conditions exist
     * When two patient allergy message arrive containing reactions
     * Then a patient allergy conditions and symptoms are added appropriate for the messages
     */
    @Test
    void testMultipleAllergyMessageProcessingWithReactions() throws EmapOperationMessageProcessingException{

        processSingleMessage(hooverMessages.get(0));
        processSingleMessage(hooverMessages.get(1));
        assertEquals(2, getAllEntities(patientConditionRepository).size());

        // Allergy diagnoses may not be associated with hospital visits
        assertEquals(0, getAllEntities(hospitalVisitRepository).size());

        // Get the second condition and check the data
        PatientCondition condition = getAllEntities(patientConditionRepository).get(1);
        assertEquals(THIRD_MRN, condition.getMrnId().getMrn());
        assertEquals(THIRD_ALLERGEN, condition.getConditionTypeId().getName());
        assertEquals(CONDITION_TYPE, condition.getConditionTypeId().getDataType());
        assertEquals(THIRD_ALLERGEN_SUBTYPE, condition.getConditionTypeId().getSubType());
        assertEquals(Instant.parse(FIRST_ADDED_TIME), condition.getAddedDatetime());
        assertEquals(LocalDate.parse(THIRD_ONSET_DATE), condition.getOnsetDate());
        assertEquals(THIRD_SEVERITY, condition.getConditionTypeId().getSeverity());
        assertEquals(ACTIVE, condition.getStatus());
        assertTrue(hasNoPriorityCommentOrResolutionTime(condition));

        // Hives should only be added once to the symptom repository
        List<ConditionSymptom> reactions = getAllEntities(conditionSymptomRepository);
        assertEquals(3, reactions.size());

        List<String> reactionNames = new ArrayList<>();
        for (ConditionSymptom reaction: reactions) {
            reactionNames.add(reaction.getName());
        }

        assertTrue(reactionNames.contains("Hives"));
    }

    /**
     * Given that a patient allergy condition exists
     * When a newer allergy message arrives that concerns the same patient
     * Then the condition is updated with the new information
     */
    @Test
    void testUpdateCommentOnNewHl7Message() throws EmapOperationMessageProcessingException {

        String testComment = "test";

        processSingleMessage(hl7Tramadol);
        assertTrue(aSingleConditionExists());
        assertNull(getAllEntities(patientConditionRepository).get(0).getComment());

        hl7Tramadol.setComment(InterchangeValue.buildFromHl7(testComment));
        hl7Tramadol.setUpdatedDateTime(Instant.parse(FIRST_UPDATED_TIME).plus(1, ChronoUnit.SECONDS));

        processSingleMessage(hl7Tramadol);
        assertEquals(testComment, firstPatientCondition().getComment());
    }

    /**
     * Given that a patient allergy condition exists
     * When an older allergy message arrives that concerns the same patient
     * Then the condition is not updated with the new information
     */
    @Test
    void testUpdateCommentOnOldHl7Message() throws EmapOperationMessageProcessingException {

        processSingleMessage(hl7Tramadol);
        assertTrue(aSingleConditionExists());
        assertNull(firstPatientCondition().getStatus());

        hl7Tramadol.setStatus(ACTIVE);
        hl7Tramadol.setUpdatedDateTime(Instant.parse(FIRST_UPDATED_TIME).minus(1, ChronoUnit.SECONDS));

        processSingleMessage(hl7Tramadol);
        assertNull(firstPatientCondition().getStatus());
    }

    /**
     * Given that a patient allergy condition exists
     * When a new allergy message arrives that concerns the same patient and adds a new reaction
     * Then the reaction is added to the list of associated reactions
     */
    @Test
    void testUpdateReactionListHl7Message() throws EmapOperationMessageProcessingException{

        processSingleMessage(hl7Tramadol);

        String reactionName = "X";
        hl7Tramadol.setReactions(List.of(new String[]{reactionName}));
        processSingleMessage(hl7Tramadol);

        assertEquals(NUM_TRAMADOL_REACTIONS+1, getAllEntities(conditionSymptomRepository).size());
        assertEquals(reactionName, getAllEntities(conditionSymptomRepository).get(0).getName());

        hl7Tramadol.setReactions(List.of(new String[]{"Y"}));
        processSingleMessage(hl7Tramadol);
        assertEquals(NUM_TRAMADOL_REACTIONS+2, getAllEntities(conditionSymptomRepository).size());
    }
}
