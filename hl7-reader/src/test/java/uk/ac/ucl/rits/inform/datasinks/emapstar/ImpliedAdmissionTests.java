package uk.ac.ucl.rits.inform.datasinks.emapstar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
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

    public ImpliedAdmissionTests() {}

    @Test
    @Transactional
    public void basicFlow() throws EmapOperationMessageProcessingException {
        queueTransfer();
        processRest();
        testState();
    }

    @Test
    @Transactional
    public void basicFlowWithVitals() throws EmapOperationMessageProcessingException {
        queueTransfer();
        queueVitals();
        processRest();
        testState();
    }

    @Test
    @Transactional
    public void vitalsThenTransfer() throws EmapOperationMessageProcessingException {
        queueVitals();
        queueTransfer();
        processRest();
        testState();
    }

    @Test
    @Transactional
    public void vitalsDischarge() throws EmapOperationMessageProcessingException {
        queueVitals();
        queueDischarge();
        processRest();
        testState();
    }

    @Test
    @Transactional
    public void vitalsCancelTransfer() throws EmapOperationMessageProcessingException {
        queueVitals();
        queueCancelTransfer();
        processRest();
        testState();
    }

    @Test
    @Transactional
    public void cancelWithoutTransferIgnored() throws EmapOperationMessageProcessingException {
        queueVitals();
        queueCancelAdmit();
        processN(1);
        assertThrows(MessageIgnoredException.class, () -> processN(1));
    }

    @Test
    @Transactional
    public void vitalsThenCancel() throws EmapOperationMessageProcessingException {
        queueVitals();
        queueCancelDischarge();
        processRest();
        testState();
    }

    @Test
    @Transactional
    public void patientUpdateRegressionBug() throws EmapOperationMessageProcessingException {
        queueVitals();
        queueUpdatePatientDetails();
        queueTransfer();
        queueUpdatePatientDetails();
        processRest();
        testState();
    }

    @Transactional
    public void testState() {
        PatientFact bedVisit = emapStarTestUtils._testVisitExistsWithLocation(this.csn, 1,
                this.allLocations[this.currentLocation], this.dischargeTime);
        // check bed visit arrival time
        List<PatientProperty> _arrivalTimes = bedVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, _arrivalTimes.size());
        PatientProperty bedArrivalTime = _arrivalTimes.get(0);
        assertEquals(this.transferTime.isEmpty() ? null : this.transferTime.get(0), bedArrivalTime.getValueAsDatetime());

        // check hospital visit arrival time
        PatientFact hospVisit = bedVisit.getParentFact();
        List<PatientProperty> _hospArrivalTimes = hospVisit.getPropertyByAttribute(AttributeKeyMap.ARRIVAL_TIME);
        assertEquals(1, _hospArrivalTimes.size());
        PatientProperty hospArrivalTime = _hospArrivalTimes.get(0);
        assertEquals(this.admissionTime, hospArrivalTime.getValueAsDatetime());
    }
}
