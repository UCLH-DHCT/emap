package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.MrnRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestType;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.identity.MrnToLive;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test cases to assert correct functionality of consultation request handling in EMAP Core.
 * @author Stef Piatek
 * @author Anika Cawthorn
 */
public class TestConsultProcessing extends MessageProcessingBase {
    @Autowired
    MrnRepository mrnRepository;
    @Autowired
    ConsultationRequestRepository consultRequestRepo;
    @Autowired
    ConsultationRequestTypeRepository consultRequestTypeRepo;
    @Autowired
    HospitalVisitRepository hospitalVisitRepository;

    private ConsultRequest minimalConsult;
    private ConsultRequest cancelledConsult;
    private ConsultRequest closedAtDischargeConsult;
    private ConsultRequest notesConsult;
    private static String FRAILTY_MRN = "40800000";
    private static Instant FRAILTY_ADD_TIME = Instant.parse("2013-02-12T11:55:00Z");
    private static String FRAILTY_CONSULTATION_TYPE = "CON255";

    @BeforeEach
    private void setUp() throws IOException {
        minimalConsult = messageFactory.getConsult("minimal.yaml");
        cancelledConsult = messageFactory.getConsult("cancelled.yaml");
        closedAtDischargeConsult = messageFactory.getConsult("closed_at_discharge.yaml");
        notesConsult = messageFactory.getConsult("notes.yaml");
    }

    /**
     * Given that no MRNS or hospital visits exist in the database
     * When a consult message is processed
     * Then minimal MRN and hospital visit should be created
     */
    @Test
    void testMinimalMrnAndHospitalVisitCreated() throws EmapOperationMessageProcessingException {
        processSingleMessage(minimalConsult);

        List<Mrn> mrns = getAllMrns();
        assertEquals(1, mrns.size());
        assertEquals(FRAILTY_MRN, mrns.get(0).getMrn());
        MrnToLive mrnToLive = mrnToLiveRepo.getByMrnIdEquals(mrns.get(0));
        assertNotNull(mrnToLive);

        HospitalVisit visit = hospitalVisitRepository.findByEncounter(defaultEncounter).orElseThrow(NullPointerException::new);

    }

    /**
     * Given that MRNs and hospital visits exist in the database
     * When a consult message is processed with existing mrn and visit number
     * Then minimal MRN and hospital visit should not be created
     */
    @Test
    void testMinimalMrnAndHospitalVisitNotCreated() throws EmapOperationMessageProcessingException{
        // processSingleMessage(minimalConsult);
    }

    /**
     * Given that no consult types exist in the database
     * When a consult message is processed
     * A new minimal consult type (only populating the code and source system, leaving the name column empty for
     * metadata hoovering) should be created
     */
    @Test
    void testMinimalConsultTypeCreated() throws EmapOperationMessageProcessingException{
        processSingleMessage(minimalConsult);
        ConsultationRequestType crType = consultRequestTypeRepo.findByStandardisedCode(
                FRAILTY_CONSULTATION_TYPE).orElseThrow();

        assertEquals(FRAILTY_ADD_TIME, crType.getValidFrom());
        assertNull(crType.getName());
    }

    /**
     * Given that no consults exist in the database
     * When a consult message is processed
     * A new consult should be created (in addition to PK and FKs should store internalConsultId, requestedDateTime, storedFrom, validFrom)
     */

    /**
     * Given that no questions and consult questions exist in the database
     * When a consult message is processed with 3 questions
     * Then 3 questions should be created and linked to 3 consult questions for the answers to the questions
     */

    /**
     * Given that no consults exist in the database
     * When a consult message is processed with notes
     * Then a new consult should be created with the notes being saved in the comments
     */

    /**
     * Given that the minimal consult has already been processed
     * When a later consult message with cancel=true with the same epicConsultId and consultationType is processed
     * Then consult should have a cancelled state or similar set to true and the storedFrom and validFrom fields update
     */

    /**
     * Given that the minimal consult has already been processed
     * When a later consult message with closedDueToDischarge=true with the same epicConsultId and consultationType is processed
     * The consult should have a closedOnDischarge state or similar set to true and the storedFrom and validFrom fields update
     */

    /**
     * Given that a minimal consult has already been processed
     * When an earlier consult message with different data is processed
     * The consult entity in question should not be updated
     */

}
