package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.VisitObservationAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.VisitObservationRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.VisitObservationTypeRepository;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservation;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationAudit;
import uk.ac.ucl.rits.inform.informdb.visit_recordings.VisitObservationType;
import uk.ac.ucl.rits.inform.interchange.Flowsheet;

import java.time.Instant;

/**
 * Interactions with observation visits.
 * @author Stef Piatek
 */
@Component
public class VisitObservationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final VisitObservationRepository visitObservationRepo;
    private final VisitObservationAuditRepository visitObservationAuditRepo;
    private final VisitObservationTypeRepository visitObservationTypeRepo;

    /**
     * @param visitObservationRepo      autowired VisitObservationRepository
     * @param visitObservationAuditRepo autowired VisitObservationAuditRepository
     * @param visitObservationTypeRepo  autowired VisitObservationTypeRepository
     */
    public VisitObservationController(VisitObservationRepository visitObservationRepo, VisitObservationAuditRepository visitObservationAuditRepo, VisitObservationTypeRepository visitObservationTypeRepo) {
        this.visitObservationRepo = visitObservationRepo;
        this.visitObservationAuditRepo = visitObservationAuditRepo;
        this.visitObservationTypeRepo = visitObservationTypeRepo;
    }

    public void processFlowsheet(Flowsheet msg, Instant storedFrom) {
        VisitObservationType observationType = getOrCreateObservationType(msg);
        RowState<VisitObservation, VisitObservationAudit> flowsheetState = getOrCreateObservation(msg, observationType, storedFrom);
        if (messageIsNewer(msg, flowsheetState)) {
            updateOrDeleteFlowsheet(flowsheetState, storedFrom);
            flowsheetState.saveEntityOrAuditLogIfRequired(visitObservationRepo, visitObservationAuditRepo);
        }
    }


    private VisitObservationType getOrCreateObservationType(Flowsheet msg) {
    }


    private RowState<VisitObservation, VisitObservationAudit> getOrCreateObservation(
            Flowsheet msg, VisitObservationType observationType, Instant storedFrom) {
    }

    private boolean messageIsNewer(Flowsheet msg, RowState<VisitObservation, VisitObservationAudit> observation) {
    }

    private void updateOrDeleteFlowsheet(RowState<VisitObservation, VisitObservationAudit> observation, Instant storedFrom) {
    }

}

