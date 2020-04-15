package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Test different transfers. Note that the Interchange ADT type
 * AdtOperationType.TRANSFER_PATIENT can originate from an A02, A06 or A07
 * HL7 message (or any transfer-like event in a non-HL7 source).
 *
 * @author Jeremy Stein
 *
 */
public class TransferTestCase extends MessageStreamTestCase {
    private List<TransferTestExpectedValues> expectedValues = new ArrayList<>();
    private Instant expectedAdmissionDateTime;

    public TransferTestCase() {
        // set up all the expected locations
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T01:01:00Z");
            locationDescription = "location1";
            patientClass = "O";
        }});
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T02:02:00Z");
            locationDescription = "location2";
            patientClass = "O";
        }});
        // redundant transfer
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T03:03:00Z");
            locationDescription = "location2";
            patientClass = "O";
            expectedRedundant = true;
        }});
        // O -> I with a location change
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T04:04:00Z");
            locationDescription = "location3";
            patientClass = "I";
        }});
        // I -> O with no location change
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T05:05:00Z");
            locationDescription = "location3";
            patientClass = "O";
            expectedRedundant = true; // this will not be true in future because the patientclass has changed
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

        @Override
        public String toString() {
            return "TransferTestExpectedValues [locationStartTime=" + locationStartTime + ", locationDescription="
                    + locationDescription + ", patientClass=" + patientClass + ", expectedRedundant="
                    + expectedRedundant + "]";
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
                        setOperationType(AdtOperationType.TRANSFER_PATIENT);
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
                assertFalse("Processor said message wasn't redundant, it should have been: " +  exp.toString(), exp.expectedRedundant);
            } catch (MessageIgnoredException me) {
                // if it threw due to a redundant transfer, check this was expected
                assertTrue("Processor said message was redundant, it shouldn't have been: " +  exp.toString(), exp.expectedRedundant);
            }
        }
    }

    @Test
    @Transactional
    public void testBedVisits() {
        // three unique locations because some are redundant
        List<PatientFact> validBedVisits = emapStarTestUtils.getLocationVisitsForEncounter("1234567890", 3);
        for (int i = 0, j = 0; i < expectedValues.size(); i++) {
            if (i > 0) {
                // If source message was for same location it shouldn't have created a new bed visit,
                // so skip the source message. This should be fixed to take account of the patient class changing too.
                String thisLocation = expectedValues.get(i).locationDescription;
                String previousMessageLocation = expectedValues.get(i - 1).locationDescription;
                if (previousMessageLocation.equals(thisLocation)) {
                    // if only patient class has changed,
                    // validBedVisits.get(j).getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS)
                    // should return multiple values
                    continue;
                }
            }
            // check the right source message against the right bed visit
            assertEquals(expectedValues.get(i).locationDescription,
                    validBedVisits.get(j).getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());
            assertEquals(expectedValues.get(i).locationStartTime,
                    validBedVisits.get(j).getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
            assertEquals(expectedValues.get(i).patientClass,
                    validBedVisits.get(j).getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).get(0).getValueAsString());
            // next bed visit
            j++;
        }
    }
}
