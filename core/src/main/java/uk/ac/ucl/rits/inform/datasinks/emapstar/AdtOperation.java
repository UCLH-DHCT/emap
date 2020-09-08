package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.informdb.Movement.LocationVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.interchange.AdtOperationType;
import uk.ac.ucl.rits.inform.interchange.adt.AdtMessageBase;

import java.time.Instant;

/**
 * Implement ADT for the Emap-Star DB v2.
 * @author Stef Piatek
 */
public class AdtOperation {
    /**
     * V2!
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private HospitalVisit onlyOpenHospitalVisit;
    private LocationVisit onlyOpenLocationVisit;
    private InformDbOperations dbOps;
    private Instant storedFrom;

    /**
     * @param dbOps      the dp ops service
     * @param adtMsg     the ADT Interchange message
     * @param storedFrom time to use for any new records that might be created
     * @throws MessageIgnoredException if message can be ignored
     */
    public AdtOperation(InformDbOperations dbOps, AdtMessageBase adtMsg, Instant storedFrom) throws MessageIgnoredException {
        this.dbOps = dbOps;
        this.storedFrom = storedFrom;
        getCreateEncounterOrVisit(dbOps, adtMsg, storedFrom);
    }

    public void getCreateEncounterOrVisit(InformDbOperations dbOps, AdtMessageBase adtMsg, Instant storedFrom)
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

    public String processMessage() {
        String returnCode = "OK";
        //process
        return returnCode;
    }
}
