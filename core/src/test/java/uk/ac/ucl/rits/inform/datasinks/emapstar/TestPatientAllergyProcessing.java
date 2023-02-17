package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.PatientConditionRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.ConditionTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.conditions.AllergenReactionRepository;

import uk.ac.ucl.rits.inform.informdb.conditions.AllergenReaction;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.interchange.ConditionAction;
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
    AllergenReactionRepository allergenReactionRepository;

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
    private static final String SECOND_ALLERGY_SEVERITY = "High";
    private static final String[] SECOND_ALLERGY_REACTIONS = {"Anaphylaxis", "Hives"};

    @BeforeEach
    private void setUp() throws IOException {
        hooverMessages = messageFactory.getPatientAllergies("updated_only.yaml");
        hl7Tramadol = messageFactory.getPatientAllergies("hl7/minimal_allergy.yaml").get(0);
    }

    private boolean aSingleConditionExists(){
        return getAllEntities(patientConditionRepository).size() == 1;
    }

    private PatientCondition getFirstPatientCondition(){
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

        PatientCondition condition = getFirstPatientCondition();
        assertEquals(FIRST_MRN, condition.getMrnId().getMrn());
        assertEquals(NUM_TRAMADOL_REACTIONS, getAllEntities(allergenReactionRepository).size());
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

        ConditionType type = conditionTypeRepository.findByDataTypeAndInternalCode(CONDITION_TYPE, FIRST_ALLERGEN).orElseThrow();

        assertEquals(FIRST_ALLERGEN, type.getInternalCode());
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

        PatientCondition condition = getFirstPatientCondition();
        assertEquals(FIRST_MRN, condition.getMrnId().getMrn());
        assertEquals(1, condition.getInternalId());
        assertEquals(SECOND_ALLERGEN, condition.getConditionTypeId().getInternalCode());
        assertEquals(CONDITION_TYPE, condition.getConditionTypeId().getDataType());
        assertEquals(SECOND_ALLERGEN_SUBTYPE, condition.getConditionTypeId().getSubType());
        assertEquals(Instant.parse(FIRST_ADDED_TIME), condition.getAddedDatetime());
        assertEquals(LocalDate.parse("2019-03-05"), condition.getOnsetDate());
        assertEquals(SECOND_ALLERGY_SEVERITY, condition.getSeverity());
        assertEquals(ACTIVE, condition.getStatus());
        assertTrue(hasNoPriorityCommentOrResolutionTime(condition));

        List<AllergenReaction> reactions = getAllEntities(allergenReactionRepository);
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
        assertEquals(2, getAllEntities(allergenReactionRepository).size());
    }

    /**
     * Given that no patient allergy conditions exist
     * When two patient allergy messages concerning two patients but with the same reaction are processed
     * Then there are two saved conditions but only a single reaction
     */
    @Test
    void testMultipleAllergyMessageProcessingWithReactions() throws EmapOperationMessageProcessingException{

        processSingleMessage(hooverMessages.get(0));
        processSingleMessage(hooverMessages.get(1));
        assertEquals(2, getAllEntities(patientConditionRepository).size());
        
        // Hives should only be added once to the symptom repository
        List<AllergenReaction> reactions = getAllEntities(allergenReactionRepository);
        assertEquals(3, reactions.size());

        assertTrue(reactions.stream().anyMatch(r -> r.getName().equals("Hives")));
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
        assertEquals(testComment, getFirstPatientCondition().getComment());
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
        assertNull(getFirstPatientCondition().getStatus());

        hl7Tramadol.setStatus(InterchangeValue.buildFromHl7(ACTIVE));
        hl7Tramadol.setUpdatedDateTime(Instant.parse(FIRST_UPDATED_TIME).minus(1, ChronoUnit.SECONDS));

        processSingleMessage(hl7Tramadol);
        assertNull(getFirstPatientCondition().getStatus());
    }

    /**
     * Given that a patient allergy message has a delete action
     * When the message is processed
     * Then the isDeleted flag on the entity is set
     */
    @Test
    void testDeletingAnAllergy() throws EmapOperationMessageProcessingException{

        hl7Tramadol.setAction(ConditionAction.DELETE);
        hl7Tramadol.setUpdatedDateTime(hl7Tramadol.getUpdatedDateTime().plus(1, ChronoUnit.SECONDS));
        processSingleMessage(hl7Tramadol);

        assertTrue(getFirstPatientCondition().getIsDeleted());
    }

    /**
     * Given there is an allergy associated with a patient
     * When an allergy message concerning the same allergy is processed but has a new reaction
     * Then only a single reaction is associated with the patient
     * @throws EmapOperationMessageProcessingException should not happen
     */
    @Test
    void testOnlyMostRecentReactionIsPresent() throws EmapOperationMessageProcessingException{

        var newReactionName = "Y";

        hl7Tramadol.setReactions(List.of("X"));
        processSingleMessage(hl7Tramadol);

        hl7Tramadol.setReactions(List.of(newReactionName));
        hl7Tramadol.setUpdatedDateTime(hl7Tramadol.getUpdatedDateTime().plus(1, ChronoUnit.SECONDS));
        processSingleMessage(hl7Tramadol);

        List<AllergenReaction> reactions = getAllEntities(allergenReactionRepository);

        assertEquals(1, reactions.size());
        assertEquals(newReactionName, reactions.get(0).getName());
    }
}
