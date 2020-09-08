package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.OldAttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Test different transfers. Note that the Interchange ADT type
 * AdtOperationType.TRANSFER_PATIENT can originate from an A02, A06 or A07 HL7
 * message (or any transfer-like event in a non-HL7 source).
 *
 * @author Jeremy Stein
 *
 */
public class TransferTestCase extends MessageStreamBaseCase {

    public TransferTestCase() {}

    @Test
    @Transactional
    public void testTransferSequence() throws EmapOperationMessageProcessingException {
        // Admit an outpatient
        queueAdmit(false, "O");
        processN(1);
        testLastBedVisit(1, currentLocation(), getPatientClass(), lastTransferTime(),  getLatestPatientClassChangeTime());

        // Transfer them
        queueTransfer();
        processN(1);
        testLastBedVisit(2, currentLocation(), getPatientClass(),  lastTransferTime(),  getLatestPatientClassChangeTime());

        // Transfer them redundantly
        queueTransfer(false, "O");
        assertThrows(MessageIgnoredException.class, () -> processN(1));

        // Transfer their class and location
        queueTransfer(true, "I");
        Instant realTransferTime =  lastTransferTime();
        processN(1);
        testLastBedVisit(3, currentLocation(), getPatientClass(), realTransferTime, getLatestPatientClassChangeTime());

        // Transfer their class only
        queueTransfer(false, "O");
        processN(1);
        testLastBedVisit(3, currentLocation(), getPatientClass(), realTransferTime, getLatestPatientClassChangeTime());

    }

    @Transactional
    public void testLastBedVisit(int expectedVisits, String thisLocation, String thisPatientClass,
            Instant thisLocationStartTime, Instant eventTime) {
        List<PatientFact> validBedVisits = emapStarTestUtils.getLocationVisitsForEncounter(this.csn, expectedVisits);

        // Get the last visit to check
        PatientFact precedingVisit = expectedVisits > 1 ? validBedVisits.get(expectedVisits - 2) : null;
        PatientFact lastVisit = validBedVisits.get(expectedVisits - 1);

        // Check right time / place / class
        assertEquals(thisLocation,
                lastVisit.getPropertyByAttribute(OldAttributeKeyMap.LOCATION).get(0).getValueAsString());
        assertEquals(thisLocationStartTime,
                lastVisit.getPropertyByAttribute(OldAttributeKeyMap.ARRIVAL_TIME).get(0).getValueAsDatetime());
        // The patient class may have changed, so make sure it is up to date
        PatientFact hospVisit = lastVisit.getParentFact();
        PatientProperty patClass = hospVisit.getPropertyByAttribute(OldAttributeKeyMap.PATIENT_CLASS).stream()
                .filter(x -> x.isValid()).findAny().get();
        assertEquals(thisPatientClass, patClass.getValueAsString());
        assertEquals(eventTime, patClass.getValidFrom());

        // Check correct invalidation date of patient class (if relevant)
        Optional<PatientProperty> invPatClass = lastVisit.getPropertyByAttribute(OldAttributeKeyMap.PATIENT_CLASS).stream()
                .filter(x -> !x.isValid()).findAny();
        if (invPatClass.isPresent()) {
            assertEquals(thisLocationStartTime, invPatClass.get().getValidFrom());
            assertEquals(eventTime, invPatClass.get().getValidUntil());
        }

        // Check that the last visit closed correctly
        if (precedingVisit != null ) {
            assertEquals(thisLocationStartTime,
                    precedingVisit.getPropertyByAttribute(OldAttributeKeyMap.DISCHARGE_TIME).get(0).getValueAsDatetime());
        }

    }
}
