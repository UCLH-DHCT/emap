package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.RequiredDataMissingException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateTypeRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.EmapOperationMessageProcessingException;
import uk.ac.ucl.rits.inform.interchange.Flowsheet;
import uk.ac.ucl.rits.inform.interchange.lab.LabOrderMsg;

import java.time.Instant;


/**
 * Interactions with patient states.
 * @author Anika Cawthorn
 */
@Component
public class PatientStateController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PatientStateRepository patientStateRepo;
    private final PatientStateTypeRepository patientStateTypeRepo;
    private final PatientStateAuditRepository patientStateAuditRepo;

    /**
     * Setting repositories holding information on patient states.
     * @param patientStateRepo      autowired PatientStateRepository
     * @param patientStateAuditRepo autowired PatientStateAuditRepository
     * @param patientStateTypeRepo  autowired PatientStateTypeRepository
     */
    public PatientStateController(
            PatientStateRepository patientStateRepo, PatientStateAuditRepository patientStateAuditRepo,
            PatientStateTypeRepository patientStateTypeRepo) {
        this.patientStateRepo = patientStateRepo;
        this.patientStateAuditRepo = patientStateAuditRepo;
        this.patientStateTypeRepo = patientStateTypeRepo;
    }

    /**
     * Process patient state message
     * @param msg        message
     * @param storedFrom Time the message started to be processed by star
     * @throws EmapOperationMessageProcessingException if message can't be processed.
     */
    @Transactional
    public void processMessage(final Flowsheet msg, final Instant storedFrom) throws EmapOperationMessageProcessingException {
        String mrnStr = msg.getMrn();

    }

}
