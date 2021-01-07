package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.datasinks.emapstar.exceptions.IncompatibleDatabaseStateException;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabBatteryTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabNumberRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabTestDefinitionRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.labs.LabNumber;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinition;
import uk.ac.ucl.rits.inform.informdb.labs.LabTestDefinitionAudit;
import uk.ac.ucl.rits.inform.interchange.LabOrder;
import uk.ac.ucl.rits.inform.interchange.LabResult;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * All interaction with labs tables.
 * @author Stef Piatek
 */
@Component
public class LabController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabNumberRepository labNumberRepo;
    private final LabTestDefinitionRepository labTestDefinitionRepo;
    private final LabBatteryTypeRepository labBatteryTypeRepo;
    private final LabOrderRepository labOrderRepo;
    private final LabResultRepository labResultRepository;

    public LabController(
            LabBatteryTypeRepository labBatteryTypeRepo, LabNumberRepository labNumberRepo, LabTestDefinitionRepository labTestDefinitionRepo,
            LabOrderRepository labOrderRepo, LabResultRepository labResultRepository) {
        this.labBatteryTypeRepo = labBatteryTypeRepo;
        this.labNumberRepo = labNumberRepo;
        this.labTestDefinitionRepo = labTestDefinitionRepo;
        this.labOrderRepo = labOrderRepo;
        this.labResultRepository = labResultRepository;
    }

    /**
     * @param mrn        MRN
     * @param visit      hospital visit
     * @param msg        order message
     * @param validFrom  time that the message was created
     * @param storedFrom time that star started processing the message
     * @throws IncompatibleDatabaseStateException if specimen type doesn't match the database
     */
    @Transactional
    public void processLabOrder(Mrn mrn, HospitalVisit visit, LabOrder msg, Instant validFrom, Instant storedFrom) throws IncompatibleDatabaseStateException {
        LabNumber labNumber = getOrCreateLabNumber(mrn, visit, msg, storedFrom);
        Set<LabTestDefinition> testDefinitions = new HashSet<>(msg.getLabResults().size());
        for (LabResult result : msg.getLabResults()) {
            RowState<LabTestDefinition, LabTestDefinitionAudit> testDefinition = updateOrCreateLabTestDefinition(result, msg, validFrom, storedFrom);
            testDefinitions.add(testDefinition.getEntity());
        }

    }

    /**
     * @param mrn        MRN
     * @param visit      hospital visit
     * @param msg        order message
     * @param storedFrom time that star started processing the message
     * @return
     * @throws IncompatibleDatabaseStateException if specimen type doesn't match the database
     */
    private LabNumber getOrCreateLabNumber(Mrn mrn, HospitalVisit visit, LabOrder msg, Instant storedFrom) throws IncompatibleDatabaseStateException {
        LabNumber labNumber = labNumberRepo
                .findByMrnIdAndHospitalVisitIdAndInternalLabNumberAndExternalLabNumber(
                        mrn, visit, msg.getEpicCareOrderNumber(), msg.getLabSpecimenNumber())
                .orElseGet(() -> createAndSaveLabNumber(mrn, visit, msg, storedFrom));

        if (!labNumber.getSpecimenType().equals(msg.getSpecimenType())) {
            throw new IncompatibleDatabaseStateException("Message specimen type doesn't match the database");
        }
        return labNumber;
    }

    private LabNumber createAndSaveLabNumber(Mrn mrn, HospitalVisit visit, LabOrder msg, Instant storedFrom) {
        logger.trace("Creating new lab number");
        LabNumber labNumber = new LabNumber(
                mrn, visit, msg.getLabSpecimenNumber(), msg.getEpicCareOrderNumber(), msg.getSpecimenType(), msg.getSourceSystem(), storedFrom);
        labNumberRepo.save(labNumber);
        return labNumber;
    }

    private RowState<LabTestDefinition, LabTestDefinitionAudit> updateOrCreateLabTestDefinition(LabResult result, LabOrder msg, Instant validFrom, Instant storedFrom) {
        RowState<LabTestDefinition, LabTestDefinitionAudit> testState = labTestDefinitionRepo
                .findByLabProviderAndLabDepartmentAndTestLabCode(
                        msg.getTestBatteryCodingSystem(), msg.getLabDepartment(), result.getTestItemLocalCode())
                .map(ltd -> new RowState<>(ltd, validFrom, storedFrom, false))
                .orElseGet(() -> {
                    LabTestDefinition testDefinition = new LabTestDefinition(
                            msg.getTestBatteryCodingSystem(), msg.getLabDepartment(), result.getTestItemLocalCode());
                    return new RowState<>(testDefinition, validFrom, storedFrom, true);
                });
    }

}
