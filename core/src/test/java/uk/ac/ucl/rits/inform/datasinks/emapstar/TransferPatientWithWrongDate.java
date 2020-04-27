package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test different transfers. Note that the Interchange ADT type
 * AdtOperationType.TRANSFER_PATIENT can originate from an A02, A06 or A07
 * HL7 message (or any transfer-like event in a non-HL7 source).
 * @author Jeremy Stein
 */
public class TransferPatientWithWrongDate extends MessageStreamTestCase {
    private List<TransferTestExpectedValues> expectedValues = new ArrayList<>();
    private Instant expectedAdmissionDateTime;


    public TransferPatientWithWrongDate() {
        // first message encountered is an update of patient info
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T01:01:00Z");
            locationDescription = "location0";
            patientClass = "E";
            operationType = AdtOperationType.UPDATE_PATIENT_INFO;
            expectedRedundant = false;
        }});
        // Transfer
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T01:01:00Z");
            locationDescription = "location1";
            patientClass = "E";
            operationType = AdtOperationType.TRANSFER_PATIENT;
        }});
        // Then cancel it
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T01:01:00Z");
            locationDescription = "location0";
            patientClass = "E";
            operationType = AdtOperationType.CANCEL_TRANSFER_PATIENT;
        }});
        // then retransfer the patient
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T01:02:00Z");
            locationDescription = "location2";
            patientClass = "E";
            operationType = AdtOperationType.TRANSFER_PATIENT;
        }});


        // admission is assumed to be the first location
        expectedAdmissionDateTime = expectedValues.get(0).locationStartTime;
    }

    /**
     * Just the changeable parts of each message.
     */
    class TransferTestExpectedValues {
        public Instant locationStartTime;
        public String locationDescription;
        public String patientClass;
        public boolean expectedRedundant;
        public AdtOperationType operationType;

        @Override
        public String toString() {
            return String.format(
                    "TransferTestExpectedValues [locationStartTime=%s, locationDescription=%s, patientClass=%s, expectedRedundant=%s, operationType=%s]",
                    locationStartTime, locationDescription, patientClass, expectedRedundant, operationType);
        }
    }

    /**
     * Generate a sequence of transfer messages.
     */
    @Before
    public void performTransfers() throws EmapOperationMessageProcessingException {
        for (TransferTestExpectedValues exp : expectedValues) {
            try {
                processSingleMessage(new AdtMessage() {
                    {
                        setOperationType(exp.operationType);
                        setAdmissionDateTime(expectedAdmissionDateTime);
                        setRecordedDateTime(exp.locationStartTime.plusSeconds(25));
                        setEventOccurredDateTime(exp.locationStartTime);
                        setMrn("1234ABCD");
                        setNhsNumber("9999999999");
                        setVisitNumber("1234567890");
                        setPatientFullName("Fred Bloggs");
                        setFullLocationString(exp.locationDescription);
                        setPatientClass(exp.patientClass);
                    }
                });
                // if processing the message didn't throw, check that it wasn't meant to throw
                assertFalse("Processor said message wasn't redundant, it should have been: " + exp.toString(), exp.expectedRedundant);
            } catch (MessageIgnoredException me) {
                // if it threw due to a redundant transfer, check this was expected
                assertTrue("Processor said message was redundant, it shouldn't have been: " + exp.toString(), exp.expectedRedundant);
            }
        }
    }

    @Test
    @Transactional
    public void testBedVisits() {
        List<PatientFact> validBedVisits = emapStarTestUtils.getLocationVisitsForEncounter("1234567890", 2);

    }
}
