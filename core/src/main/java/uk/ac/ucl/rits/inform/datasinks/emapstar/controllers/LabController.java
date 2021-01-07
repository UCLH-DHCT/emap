package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabBatteryTypeRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabNumberRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.LabResultRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.interchange.LabOrder;

import java.time.Instant;

/**
 * All interaction with labs tables.
 * @author Stef Piatek
 */
@Component
public class LabController {
    private final LabBatteryTypeRepository labBatteryTypeRepo;
    private final LabNumberRepository labNumberRepo;
    private final LabOrderRepository labOrderRepo;
    private final LabResultRepository labResultRepository;

    public LabController(
            LabBatteryTypeRepository labBatteryTypeRepo, LabNumberRepository labNumberRepo,
            LabOrderRepository labOrderRepo, LabResultRepository labResultRepository) {
        this.labBatteryTypeRepo = labBatteryTypeRepo;
        this.labNumberRepo = labNumberRepo;
        this.labOrderRepo = labOrderRepo;
        this.labResultRepository = labResultRepository;
    }

    public void processLabOrder(Mrn mrn, HospitalVisit visit, LabOrder msg, Instant validFrom, Instant storedFrom) {

    }
}
