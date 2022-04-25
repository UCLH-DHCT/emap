package uk.ac.ucl.rits.inform.datasinks.emapstar.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.ConsultationRequestRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.HospitalVisitRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabBatteryRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabOrderRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabResultRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleAuditRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs.LabSampleRepository;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequest;
import uk.ac.ucl.rits.inform.informdb.consults.ConsultationRequestAudit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisitAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrder;
import uk.ac.ucl.rits.inform.informdb.labs.LabOrderAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabResultAudit;

import java.time.Instant;
import java.util.List;

/**
 * @author Jeremy Stein
 *
 * Operations to delete entities in a cascading fashion, creating new audit rows as appropriate.
 * This was written with performing unfiltered, cascading deletes in mind
 * eg. delete an entire person's record.
 * It could be adapted to be more selective (but still be cascading).
 */
@Component
public class DeletionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LabBatteryRepository labBatteryRepo;
    private final LabSampleRepository labSampleRepo;
    private final LabSampleAuditRepository labSampleAuditRepo;
    private final LabOrderRepository labOrderRepo;
    private final LabOrderAuditRepository labOrderAuditRepo;
    private final LabResultRepository labResultRepo;
    private final LabResultAuditRepository labResultAuditRepo;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final ConsultationRequestAuditRepository consultationRequestAuditRepository;
    private final HospitalVisitRepository hospitalVisitRepo;
    private final HospitalVisitAuditRepository hospitalVisitAuditRepo;

    /**
     * Deletion controller needs access to pretty much every repo in order to do cascading deletes.
     *
     * @param labBatteryRepo lab battery repo
     * @param labSampleRepo lab sample repo
     * @param labSampleAuditRepo lab sample audit repo
     * @param labOrderRepo lab order repo
     * @param labOrderAuditRepo lab order audit repo
     * @param labResultRepo lab result repo
     * @param labResultAuditRepo lab result audit repo
     * @param consultationRequestRepo consultation request repo
     * @param consultationRequestAuditRepo consultation request audit repo
     * @param hospitalVisitRepo hospital visit repo
     * @param hospitalVisitAuditRepo hospital visit audit repo
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public DeletionController(
           LabBatteryRepository labBatteryRepo, LabSampleRepository labSampleRepo,
           LabSampleAuditRepository labSampleAuditRepo,
           LabOrderRepository labOrderRepo, LabOrderAuditRepository labOrderAuditRepo,
           LabResultRepository labResultRepo, LabResultAuditRepository labResultAuditRepo,
           ConsultationRequestRepository consultationRequestRepo, ConsultationRequestAuditRepository consultationRequestAuditRepo,
           HospitalVisitRepository hospitalVisitRepo, HospitalVisitAuditRepository hospitalVisitAuditRepo
    ) {
        this.labBatteryRepo = labBatteryRepo;
        this.labSampleRepo = labSampleRepo;
        this.labSampleAuditRepo = labSampleAuditRepo;
        this.labOrderRepo = labOrderRepo;
        this.labOrderAuditRepo = labOrderAuditRepo;
        this.labResultRepo = labResultRepo;
        this.labResultAuditRepo = labResultAuditRepo;
        this.consultationRequestRepository = consultationRequestRepo;
        this.consultationRequestAuditRepository = consultationRequestAuditRepo;
        this.hospitalVisitRepo = hospitalVisitRepo;
        this.hospitalVisitAuditRepo = hospitalVisitAuditRepo;
    }

    /**
     * Delete all visits that are older than the current message.
     * @param visits     List of hopsital visits
     * @param invalidationTime  Time of the delete information message
     * @param deletionTime time that emap-core started processing the message.
     */
    public void deleteVisits(Iterable<HospitalVisit> visits, Instant invalidationTime, Instant deletionTime) {
        for (HospitalVisit visit : visits) {
            deleteLabOrdersForVisit(visit, invalidationTime, deletionTime);
            deleteConsultRequestsForVisit(visit, invalidationTime, deletionTime);

            hospitalVisitAuditRepo.save(new HospitalVisitAudit(visit, invalidationTime, deletionTime));
            hospitalVisitRepo.delete(visit);
        }
    }

    private void deleteLabOrdersForVisit(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        List<LabOrder> labOrders = labOrderRepo.findByHospitalVisitId(visit);
        for (var lo : labOrders) {
            deleteLabResultsForLabOrder(lo, invalidationTime, deletionTime);

            LabOrderAudit labOrderAudit = lo.createAuditEntity(invalidationTime, deletionTime);
            labOrderAuditRepo.save(labOrderAudit);
            labOrderRepo.delete(lo);
        }

    }

    private void deleteLabResultsForLabOrder(LabOrder labOrder, Instant invalidationTime, Instant deletionTime) {
        List<LabResult> labResults = labResultRepo.findByLabOrderId(labOrder);
        for (var lr : labResults) {
            LabResultAudit resultAudit = lr.createAuditEntity(invalidationTime, deletionTime);
            labResultAuditRepo.save(resultAudit);
            labResultRepo.delete(lr);
        }
    }

    private void deleteConsultRequestsForVisit(HospitalVisit visit, Instant invalidationTime, Instant deletionTime) {
        List<ConsultationRequest> consultationRequests = consultationRequestRepository.findByHospitalVisitId(visit);
        for (var cr : consultationRequests) {
            ConsultationRequestAudit auditEntity = cr.createAuditEntity(invalidationTime, deletionTime);
            consultationRequestAuditRepository.save(auditEntity);
            consultationRequestRepository.delete(cr);
        }
    }

}
