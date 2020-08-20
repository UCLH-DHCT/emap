package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.AttributeKeyMap;
import uk.ac.ucl.rits.inform.informdb.PatientFact;
import uk.ac.ucl.rits.inform.informdb.Movement.LocationVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;

/**
 * Implement ADT for the Emap-Star DB v2.
 *
 * @author Jeremy Stein
 *
 */
public class AdtOperationDbV2 implements AdtOperationInterface {
    /**
     * V2!
     */
    private HospitalVisit onlyOpenHospitalVisit;
    private LocationVisit onlyOpenLocationVisit;

    public AdtOperationDbV2(InformDbOperations informDbOperations, AdtMessage adtMsg, Instant storedFrom) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public String processMessage() throws MessageIgnoredException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void getCreateEncounterOrVisit(InformDbOperations dbOps, AdtMessage adtMsg, Instant storedFrom)
            throws MessageIgnoredException {
        if (adtMsg.getVisitNumber() != null) {
            // V2
            // need a "getcreate" here
            onlyOpenHospitalVisit = dbOps.findHospitalVisitByEncounter(adtMsg.getVisitNumber());
            // onlyOpenLocationVisit = ... ;
        } else if (!adtMsg.getOperationType().equals(AdtOperationType.MERGE_BY_ID)) {
            // CSNs are not present in merge by ID messages, but in other messages this is an error
            throw new MessageIgnoredException(adtMsg, "CSN missing in a non-merge message: " + adtMsg.getOperationType());
        }
    }

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
    @Override
    public boolean ensureAdmissionExists() throws MessageIgnoredException {
        // This perhaps belongs in a getCreateHospitalVisit method, with an
        // InformDbDataIntegrity exception

        // NEED TO do a find first
        HospitalVisit hospitalVisit2 = new HospitalVisit();
//        hospitalVisit2.setEncounter(encounter);
        if (onlyOpenLocationVisit == null) {
            PatientFact hospitalVisit = InformDbOperations.addOpenHospitalVisit(encounter, storedFrom, admissionDateTime, adtMsg.getPatientClass());
            // create a new location visit with the new (or updated) location
            AttributeKeyMap visitType = InformDbOperations.visitTypeFromPatientClass(adtMsg.getPatientClass());
            AdtOperation.addOpenLocationVisit(encounter, visitType, storedFrom, locationVisitValidFrom, locationVisitStartTime, hospitalVisit,
                    adtMsg.getFullLocationString(), adtMsg.getPatientClass());
            encounter = dbOps.save(encounter);
            logger.info(String.format("Encounter: %s", encounter.toString()));
            onlyOpenBedVisit = InformDbOperations.getOnlyElement(InformDbOperations.getOpenValidLocationVisit(encounter));
            return true;
        }
        return false;
    }


    @Override
    public boolean ensureLocation() throws MessageIgnoredException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void performAdmit() throws MessageIgnoredException {
        // TODO Auto-generated method stub

    }

    @Override
    public void performTransfer() throws MessageIgnoredException {
        // TODO Auto-generated method stub

    }

    @Override
    public void performDischarge() throws MessageIgnoredException {
        // TODO Auto-generated method stub

    }

    @Override
    public void performCancelAdmit() throws MessageIgnoredException {
        // TODO Auto-generated method stub

    }

    @Override
    public void performCancelTransfer() throws MessageIgnoredException {
        // TODO Auto-generated method stub

    }

    @Override
    public void performCancelDischarge() throws MessageIgnoredException {
        // TODO Auto-generated method stub

    }

    @Override
    public void performMergeById() throws MessageIgnoredException {
        // TODO Auto-generated method stub

    }

}
