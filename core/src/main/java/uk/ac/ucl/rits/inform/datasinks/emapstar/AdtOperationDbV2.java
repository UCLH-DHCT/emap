package uk.ac.ucl.rits.inform.datasinks.emapstar;

import java.time.Instant;

import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.MessageIgnoredException;
import uk.ac.ucl.rits.inform.interchange.AdtMessage;

/**
 * Implement ADT for the Emap-Star DB v2.
 *
 * @author Jeremy Stein
 *
 */
public class AdtOperationDbV2 implements AdtOperationInterface {

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
        // TODO Auto-generated method stub

    }

    @Override
    public boolean ensureAdmissionExists() throws MessageIgnoredException {
        // TODO Auto-generated method stub
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
