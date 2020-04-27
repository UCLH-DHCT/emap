package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.VitalSigns;

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
public class VitalsUpdateTransferTestCase extends MessageStreamTestCase {
    private List<TransferTestExpectedValues> expectedValues = new ArrayList<>();
    private Instant expectedAdmissionDateTime;


    public VitalsUpdateTransferTestCase() {
        // first message encountered is an update of patient info
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T01:01:00Z");
            locationDescription = "location0";
            operationType = AdtOperationType.UPDATE_PATIENT_INFO;
            expectedRedundant = false;
            patientName = "Joe Bloggs";
        }});
        // Transfer
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T01:01:00Z");
            locationDescription = "location1";
            operationType = AdtOperationType.TRANSFER_PATIENT;
            patientName = "Joe Bloggs";
        }});
        // Change the patient details after transfer
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T01:01:00Z");
            operationType = AdtOperationType.UPDATE_PATIENT_INFO;
            expectedRedundant = false;
            patientName = "Joe B";
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
        public String patientName;

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
        // manually add vitalsign for implied admission
        processSingleMessage(new VitalSigns() {{
            setMrn("1234ABCD");
            setVisitNumber("1234567890");
            setVitalSignIdentifier("HEART_RATE");
            setNumericValue(92.);
            setUnit("/min");
            setObservationTimeTaken(Instant.parse("2000-01-01T01:01:00Z"));
        }});

        // process ADT messages
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
                        setPatientFullName(exp.patientName);
                        setFullLocationString(exp.locationDescription);
                        setPatientClass("I");
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
        List<PatientFact> validBedVisits = emapStarTestUtils.getLocationVisitsForEncounter("1234567890", 1);

    }
}
