package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.OldAdtMessage;

public interface AdtOperationInterface {

    /**
     * Go ahead and process the ADT message.
     * @return a status string
     *
     * @throws MessageIgnoredException if message is being ignored
     */
    String processMessage() throws MessageIgnoredException;

    /**
     * Get or create the encounter and/or visit, as appropriate for the implementation.
     * @param dbOps      the dp ops service
     * @param adtMsg     the ADT Interchange message
     * @param storedFrom the storedFrom time to use if an object needs to be newly created
     * @throws MessageIgnoredException
     */
    void getCreateEncounterOrVisit(InformDbOperations dbOps, OldAdtMessage adtMsg, Instant storedFrom)
            throws MessageIgnoredException;

    /**
     * Create a new encounter using the details given in the ADT message. This may
     * also entail creating a new Mrn and Person if these don't already exist.
     * This may occur as the result of not just an A01/A04 message, because A02 or
     * A03 messages can also trigger an "admit" if we didn't previously know about that patient.
     * For this reason, look more at the patient class than whether it's an A01 or A04
     * when determining whether to create a BED_VISIT instead of an OUTPATIENT_VISIT.
     *
     * ED flows tend to go A04+A08+A01 (all patient class = E). A04 and A01 are both considered
     * to be admits here, so treat this as a transfer from the A04 to the A01 location.
     * Note that this now breaks the previous workaround for the A01+(A11)+A01 sequence before the
     * A11 messages were added to the feed, which treated A01+A01 as the second A01 *correcting* the first's
     * patient location. Now we require an explicit A11 to count it as a correction,
     * which I think is OK as these got turned on in July 2019.
     * @return true iff a new visit was actually created
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    boolean ensureAdmissionExists() throws MessageIgnoredException;

    /**
     * Assumes that an admission already exists. If location needs to be updated, then perform a transfer.
     * @return true iff anything was changed
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    boolean ensureLocation() throws MessageIgnoredException;

    /**
     * Admit a patient, checking if already admitted first.
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    void performAdmit() throws MessageIgnoredException;

    /**
     * Transfer a patient, recognising that any, none or all of the following may
     * have changed: location, patient class, demographics, and the admission may or
     * may not exist.
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    void performTransfer() throws MessageIgnoredException;

    /**
     * Mark the specified visit as finished.
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    void performDischarge() throws MessageIgnoredException;

    /**
     * Cancel a pre-existing admission by invalidating the facts associated with it.
     * @throws MessageIgnoredException if message can't be processed
     */
    void performCancelAdmit() throws MessageIgnoredException;

    /**
     * Cancel the most recent bed visit by invalidating it.
     *
     * @throws MessageIgnoredException    if message can't be processed
     */
    void performCancelTransfer() throws MessageIgnoredException;

    /**
     * Mark the visit specified by visit number as not discharged any more. Can either mean a discharge was
     * erroneously entered, or a decision to discharge was reversed.
     *
     * @throws MessageIgnoredException if message can't be processed
     */
    void performCancelDischarge() throws MessageIgnoredException;

    /**
     * Indicate in the DB that two MRNs now belong to the same person. One MRN is
     * designated the surviving MRN, although we can't prevent data being added to
     * whichever MRN/CSN is specified in future messages, which (if the source
     * system is behaving) we'd hope would be the surviving MRN. The best we could
     * do is flag it as an error if new data is put against a non-surviving MRN.
     *
     * @throws MessageIgnoredException if merge time in message is blank or message
     *                                 can't be processed
     */
    void performMergeById() throws MessageIgnoredException;

}
