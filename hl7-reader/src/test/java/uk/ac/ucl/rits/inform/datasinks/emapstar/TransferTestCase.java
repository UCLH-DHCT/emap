package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
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
            locationDescription = "location0";
            patientClass = "O";
        }});
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T02:02:00Z");
            locationDescription = "location1";
            patientClass = "O";
        }});
        // completely redundant transfer
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T03:03:00Z");
            locationDescription = "location1";
            patientClass = "O";
            expectedRedundant = true;
        }});
        // O -> I with a location change
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T04:04:00Z");
            locationDescription = "location2";
            patientClass = "I";
        }});
        // I -> O with no location change
        expectedValues.add(new TransferTestExpectedValues() {{
            locationStartTime = Instant.parse("2001-01-01T05:05:00Z");
            locationDescription = "location2";
            patientClass = "O";
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
        // three unique locations expected because some are redundant
        List<PatientFact> validBedVisits = emapStarTestUtils.getLocationVisitsForEncounter("1234567890", 3);
        for (int i = 0, j = -1; i < expectedValues.size(); i++) {
            boolean locationChanged = true;
            boolean patientClassChanged = true;
            String thisLocation = expectedValues.get(i).locationDescription;
            String thisPatientClass = expectedValues.get(i).patientClass;
            String previousPatientClass = null;
            if (i > 0) {
                String previousLocation = expectedValues.get(i - 1).locationDescription;
                locationChanged = !previousLocation.equals(thisLocation);
                // If source message was for same location it shouldn't have created a new bed visit,
                // but if the patientclass has changed, check for patient class updates.
                previousPatientClass = expectedValues.get(i - 1).patientClass;
                patientClassChanged = !thisPatientClass.equals(previousPatientClass);
            }
            if (locationChanged) {
                // Only a location change triggers a new bed visit in star
                j++;
            }

            // Iff nothing has changed, it should have been intended to be redundant
            assertEquals(!locationChanged && !patientClassChanged, expectedValues.get(i).expectedRedundant);

            // Do checks depending on what should have changed. Possibly nothing has changed if
            // it was a redundant transfer, so do no checks.
            if (!locationChanged && patientClassChanged) {
                // Patient class has changed, although location hasn't.
                // So check for multiple patient class properties (old and new).
                Map<Boolean, List<PatientProperty>> patientClasses = validBedVisits.get(j)
                        .getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).stream()
                        .collect(Collectors.groupingBy(p -> p.getValidUntil() == null));
                List<PatientProperty> valids = patientClasses.get(true);
                List<PatientProperty> invalids = patientClasses.get(false);
                assertEquals(1, valids.size());
                assertEquals(1, invalids.size()); // doesn't support more than one class-change-only transfer in a row!
                assertEquals(previousPatientClass, invalids.get(0).getValueAsString());
                assertEquals(thisPatientClass, valids.get(0).getValueAsString());
                Instant thisStartTime = expectedValues.get(i).locationStartTime;
                Instant previousStartTime = expectedValues.get(i - 1).locationStartTime;
                // check that properties have been invalidated at the right time
                assertEquals(previousStartTime, invalids.get(0).getValidFrom());
                assertEquals(thisStartTime, invalids.get(0).getValidUntil());
                assertEquals(thisStartTime, valids.get(0).getValidFrom());
            } else if (locationChanged) {
                // Location has changed, there should have been a proper transfer so check for that.
                // Check the right source message against the right bed visit.
                assertEquals(expectedValues.get(i).locationDescription,
                        validBedVisits.get(j).getPropertyByAttribute(AttributeKeyMap.LOCATION).get(0).getValueAsString());
                assertEquals(expectedValues.get(i).locationStartTime,
                        validBedVisits.get(j).getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
                assertEquals(expectedValues.get(i).patientClass,
                        validBedVisits.get(j).getPropertyByAttribute(AttributeKeyMap.PATIENT_CLASS).get(0).getValueAsString());
            }
        }
    }
}
