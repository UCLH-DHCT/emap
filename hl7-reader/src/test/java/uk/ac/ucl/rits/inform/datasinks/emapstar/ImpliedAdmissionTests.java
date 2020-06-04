package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.PatientProperty;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;

/**
 * Tests for transfers where the message streams starts without an inital
 * admission
 *
 * @author Jeremy Stein
 *
 */
public class ImpliedAdmissionTests extends MessageStreamBaseCase {

    private int totalExpectedLocations = 1;

    public ImpliedAdmissionTests() {}

    @Test
    @Transactional
    public void basicFlow() throws EmapOperationMessageProcessingException {
        queueTransfer();
        processRest();
    }

    @Test
    @Transactional
    public void basicFlowWithVitals() throws EmapOperationMessageProcessingException {
        queueTransfer();
        queueVital();
        processRest();
    }

    @Test
    @Transactional
    public void vitalsThenTransfer() throws EmapOperationMessageProcessingException {
        queueVital();
        queueTransfer();
        processRest();
    }

    @Test
    @Transactional
    public void vitalsDischarge() throws EmapOperationMessageProcessingException {
        queueVital();
        queueDischarge();
        processRest();
    }

    @Test
    @Transactional
    public void vitalsCancelTransfer() throws EmapOperationMessageProcessingException {
        queueVital();
        queueCancelTransfer();
        processRest();
    }

    @Test
    @Transactional
    public void cancelWithoutTransferIgnored() throws EmapOperationMessageProcessingException {
        queueVital();
        queueCancelAdmit();
        queueTransfer();
        processRest();
    }

    @Test
    @Transactional
    public void vitalsThenCancel() throws EmapOperationMessageProcessingException {
        queueVital();
        queueCancelDischarge();
        processRest();
    }

    @Test
    @Transactional
    public void patientUpdateRegressionBug() throws EmapOperationMessageProcessingException {
        totalExpectedLocations  = 2;
        queueVital();
        queueUpdatePatientDetails();
        queueTransfer();
        queueUpdatePatientDetails();
        processRest();
    }

    @AfterEach
    @Transactional
    public void testState() {
        PatientFact bedVisit = emapStarTestUtils._testVisitExistsWithLocation(this.csn, totalExpectedLocations,
                this.allLocations[this.currentLocation], this.dischargeTime);
        // check bed visit arrival time
        List<PatientProperty> _arrivalTimes = bedVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, _arrivalTimes.size());
        PatientProperty bedArrivalTime = _arrivalTimes.get(0);
        Instant expectedArrivalTime;
        if (this.transferTime.isEmpty() || this.transferTime.get(0).equals(Instant.MIN)) {
            expectedArrivalTime = null;
        } else {
            // looking at current location, so use last transfer time
            expectedArrivalTime = this.transferTime.get(this.transferTime.size() - 1);
        }
        assertEquals(expectedArrivalTime, bedArrivalTime.getValueAsDatetime());

        // check hospital visit arrival time
        PatientFact hospVisit = bedVisit.getParentFact();
        List<PatientProperty> _hospArrivalTimes = hospVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, _hospArrivalTimes.size());
        PatientProperty hospArrivalTime = _hospArrivalTimes.get(0);
        assertEquals(this.admissionTime, hospArrivalTime.getValueAsDatetime());
    }
}
