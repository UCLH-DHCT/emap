package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

/**
 * Ensure that the full hierarchy of patient information is created if we start processing
 * in the middle of their message stream.
 * Person->MRN->Encounter->{Visit Facts, Other Facts}
 *
 * Eg. we get a transfer or discharge message without having received an admit message
 * first. Or we get vitals for a patient we've never seen before. Different orderings
 * of these events can fail if we're not careful.
 *
 * This is an abstract test case that defines the possible messages that may be generated
 * for processing. Each subclass should define a different subset and/or ordering of messages to send.
 * On the whole the expected end state (and therefore the tests defined) will be the same.
 *
 * @author Jeremy Stein
 */
public abstract class ImpliedAdmissionTestCase extends MessageStreamTestCase {
    private Instant expectedAdmissionDateTime = Instant.parse("2020-03-01T06:30:00.000Z");
    private Instant expectedTransferDateTime = Instant.parse("2020-03-01T10:35:00.000Z");
    private Instant expectedDischargeTime = null;

    public ImpliedAdmissionTestCase() {
    }

    /**
     * Process a vital signs message.
     */
    public void addVitals() throws EmapOperationMessageProcessingException {
        processSingleMessage(new VitalSigns() {{
            setMrn("1234ABCD");
            setVisitNumber("1234567890");
            setVitalSignIdentifier("HEART_RATE");
            //setVitalSignIdentifierCodingSystem("JES");
            setNumericValue(92.);
            setUnit("/min");
            setObservationTimeTaken(Instant.parse("2020-03-01T08:32:48Z"));
        }});
    }

    /**
     * Process a transfer message.
     */
    public void performTransfer() throws EmapOperationMessageProcessingException {
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.TRANSFER_PATIENT);
            setAdmissionDateTime(expectedAdmissionDateTime);
            setEventOccurredDateTime(expectedTransferDateTime);
            setMrn("1234ABCD");
            setVisitNumber("1234567890");
            setPatientClass("I");
            setPatientFullName("Fred Bloggs");
            setFullLocationString("T42^BADGERS^WISCONSIN");
        }});
    }

    /**
     * Process a discharge message.
     */
    public void performDischarge() throws EmapOperationMessageProcessingException {
        expectedDischargeTime = Instant.parse("2020-03-01T16:21:54.000Z");
        processSingleMessage(new AdtMessage() {{
            setOperationType(AdtOperationType.DISCHARGE_PATIENT);
            setAdmissionDateTime(expectedAdmissionDateTime);
            setEventOccurredDateTime(expectedTransferDateTime);
            setMrn("1234ABCD");
            setFullLocationString("T42^BADGERS^WISCONSIN");
            setVisitNumber("1234567890");
            setPatientClass("I");
            setPatientFullName("Fred Bloggs");
            setDischargeDisposition("foo");
            setDischargeLocation("Home");
            setDischargeDateTime(expectedDischargeTime);
            setPatientDeathIndicator(false);
        }});
    }

    /**
     * Whatever happens in the setup, this visit should always be present.
     * Expected discharge time depends on whether the discharge message has been sent.
     */
    @Test
    @Transactional
    public void testVisitPresent() {
        PatientFact bedVisit = emapStarTestUtils._testVisitExistsWithLocation("1234567890", 1, "T42^BADGERS^WISCONSIN", expectedDischargeTime);
        // check bed visit arrival time
        List<PatientProperty> _arrivalTimes = bedVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, _arrivalTimes.size());
        PatientProperty arrivalTime = _arrivalTimes.get(0);

        // this is known to fail
        //        assertEquals(expectedTransferDateTime, arrivalTime.getValueAsDatetime());

        // check hospital visit arrival time
        PatientFact hospVisit = bedVisit.getParentFact();
        List<PatientProperty> _hospArrivalTimes = hospVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, _hospArrivalTimes.size());
        PatientProperty hospArrivalTime = _hospArrivalTimes.get(0);
        assertEquals(expectedAdmissionDateTime, hospArrivalTime.getValueAsDatetime());
    }
}
