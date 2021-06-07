package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestTypeRepository;
import uk.ac.ucl.rits.inform.interchange.ConsultRequest;

import java.io.IOException;
import java.time.Instant;

public class TestConsultProcessing extends MessageProcessingBase {
    @Autowired
    ConsultationRequestRepository consultRequestRepo;
    @Autowired
    ConsultationRequestTypeRepository consultRequestTypeRepo;

    private ConsultRequest minimalConsult;
    private ConsultRequest cancelledConsult;
    private ConsultRequest closedAtDischargeConsult;
    private ConsultRequest notesConsult;
    private static String FRAILTY_MRN = "40800000";
    private static Instant FRAILTY_ADD_TIME = Instant.parse("2019-03-07T11:31:05Z");
    private static String FRAILTY_CONSULTATION_TYPE = "CON225";

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
    void testMinimalMrnAndHospitalVisitCreated() {
        System.out.println(minimalConsult);
    }

    /**
     * Given that MRNs and hospital visits exist in the database
     * When a consult message is processed with existing mrn and visit number
     * Then minimal MRN and hospital visit should not be created
     */
    void testMinimalMrnAndHospitalVisitNotCreated(){
        System.out.println("test minimal Mrn and hospital visit not created");
    }

    /**
     * Given that no consult types exist in the database
     * When a consult message is processed
     * A new minimal consult type (only populating the code and source system, leaving the name column empty for metadata hoovering) should be created
     */

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
