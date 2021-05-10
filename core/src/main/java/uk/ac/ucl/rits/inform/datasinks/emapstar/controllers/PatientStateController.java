package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.PatientStateTypeRepository;



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


}
