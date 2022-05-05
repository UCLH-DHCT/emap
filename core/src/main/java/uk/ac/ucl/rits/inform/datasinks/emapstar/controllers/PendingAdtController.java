package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.interchange.adt.CancelPendingTransfer;
import uk.ac.ucl.rits.inform.interchange.adt.PendingTransfer;

import java.time.Instant;

/**
 * Carries out the business logic part of pending ADT messages.
 * This could be added to the patient location controller, but it seemed big enough already.
 * When we have ADT edits (ADT^Z99) set up, and potentially the individual movement IDs in hl7 messages,
 * it may be worth thinking about the structure - hopefully less out-of-order processing would be required so the location visit should be simpler.
 * @author Stef Piatek
 */
@Component
public class PendingAdtController {


    public void processMsg(HospitalVisit visit, PendingTransfer msg, Instant validFrom, Instant storedFrom) {
        return;
    }

    public void processMsg(HospitalVisit visit, CancelPendingTransfer msg, Instant validFrom, Instant storedFrom) {
        return;
    }
}
