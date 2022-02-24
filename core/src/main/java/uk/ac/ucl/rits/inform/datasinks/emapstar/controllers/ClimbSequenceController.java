package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.ClimbSequenceAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.ClimbSequenceRepository;
import uk.ac.ucl.rits.inform.informdb.labs.ClimbSequence;
import uk.ac.ucl.rits.inform.informdb.labs.ClimbSequenceAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabSample;
import uk.ac.ucl.rits.inform.interchange.lab.ClimbSequenceMsg;

import java.time.Instant;

@Component
public class ClimbSequenceController {
    private final ClimbSequenceRepository climbSequenceRepo;
    private final ClimbSequenceAuditRepository climbSequenceAuditRepo;

    /**
     * @param climbSequenceRepo  interact with climb sequence table
     * @param climbSequenceAuditRepo interact with climb sequence audit table
     */
    ClimbSequenceController(ClimbSequenceRepository climbSequenceRepo, ClimbSequenceAuditRepository climbSequenceAuditRepo) {
        this.climbSequenceRepo = climbSequenceRepo;
        this.climbSequenceAuditRepo = climbSequenceAuditRepo;
    }

    /**
     * Process a climb sequence message.
     * @param msg        climb sequence data
     * @param labSample  if present then it is a hospital sequence, will be null for a community sequence
     * @param validFrom  most recent change to the sequence
     * @param storedFrom time that star started processing the message
     */
    public void processSequence(ClimbSequenceMsg msg, LabSample labSample, Instant validFrom, Instant storedFrom) {
        RowState<ClimbSequence, ClimbSequenceAudit> sequenceState = climbSequenceRepo
                .findByPheIdAndLabSampleId(msg.getPheId(), labSample)
                .map(seq -> new RowState<>(seq, validFrom, storedFrom, false))
                .orElseGet(() -> {
                    ClimbSequence seq = new ClimbSequence(labSample, msg.getPheId());
                    return new RowState<>(seq, validFrom, storedFrom, true);
                });
        // update fields if they are newer and different
        ClimbSequence seq = sequenceState.getEntity();
        Instant entityFrom = seq.getValidFrom();
        sequenceState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getFastaHeader(), seq.getFastaHeader(), seq::setFastaHeader, validFrom, entityFrom
        );
        sequenceState.assignIfCurrentlyNullOrNewerAndDifferent(
                msg.getSampleCollectionDate(), seq.getCollectionDate(), seq::setCollectionDate, validFrom, entityFrom
        );
        sequenceState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getSequence(), seq.getSequence(), seq::setSequence, validFrom, entityFrom);
        sequenceState.assignIfCurrentlyNullOrNewerAndDifferent(msg.getCogId(), seq.getCogId(), seq::setCogId, validFrom, entityFrom);

        // save tables if required
        sequenceState.saveEntityOrAuditLogIfRequired(climbSequenceRepo, climbSequenceAuditRepo);
    }
}
