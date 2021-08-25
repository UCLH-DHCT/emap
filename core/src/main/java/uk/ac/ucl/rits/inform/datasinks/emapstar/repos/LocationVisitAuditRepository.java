package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.Location;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisitAudit;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Audit hospital visit repository.
 * @author Stef Piatek
 */
public interface LocationVisitAuditRepository extends CrudRepository<LocationVisitAudit, Long> {
    /**
     * For testing: find by location string.
     * @param location full location string
     * @return AuditLocationVisit wrapped in optional
     */
    Optional<LocationVisitAudit> findByLocationIdLocationString(String location);

    /**
     * Is a message location cancelled.
     * @param hospitalVisit      hospital visit
     * @param location           location
     * @param messageTime        time of message
     * @param dischargeCancelled true if message is a discharge message
     * @return true if message has been cancelled.
     */
    default Boolean messageLocationIsCancelled(HospitalVisit hospitalVisit, Location location, Instant messageTime, boolean dischargeCancelled) {
        if (dischargeCancelled) {
            return existsByHospitalVisitIdAndLocationIdAndAdmissionTimeAndDischargeTimeAndInferredDischargeIsFalse(
                    hospitalVisit.getHospitalVisitId(), location, messageTime, messageTime);
        }
        return existsByHospitalVisitIdAndLocationIdAndAdmissionTimeAndDischargeTimeAndInferredAdmissionIsFalse(
                hospitalVisit.getHospitalVisitId(), location, messageTime, messageTime);
    }

    Boolean existsByHospitalVisitIdAndLocationIdAndAdmissionTimeAndDischargeTimeAndInferredAdmissionIsFalse(
            Long hospitalVisit, Location location, Instant admission, Instant discharge);

    Boolean existsByHospitalVisitIdAndLocationIdAndAdmissionTimeAndDischargeTimeAndInferredDischargeIsFalse(
            Long hospitalVisit, Location location, Instant admission, Instant discharge);

    Optional<LocationVisitAudit> findByHospitalVisitIdAndLocationIdAndAdmissionTimeAndDischargeTime(
            Long hospitalVisitId, Location location, Instant admissionTime, Instant dischargeTime);

    /**
     * Find a potential location visit audit for a specific discharge time.
     * @param locationVisitId  ID for location visit
     * @param cancellationTime time of potentially previously cancelled discharge
     * @return potential LocationVisitAudit entity
     */
    default Optional<LocationVisitAudit> findPreviousLocationVisitAuditForDischarge(Long locationVisitId, Instant cancellationTime) {
        List<LocationVisitAudit> audits = findAllByLocationVisitIdOrderByStoredUntilDesc(locationVisitId);

        Set<Instant> cancelledDischarges = new HashSet<>();
        cancelledDischarges.add(cancellationTime);
        for (LocationVisitAudit audit : audits) {
            if (audit.getDischargeTime() == null || audit.getAdmissionTime() == null) {
                continue;
            }
            if (audit.getDischargeTime().equals(audit.getAdmissionTime()) && !audit.getInferredAdmission()) {
                cancelledDischarges.add(audit.getDischargeTime());
            }
        }

        LocationVisitAudit previousDischarge = null;
        for (LocationVisitAudit audit : audits) {
            if (cancelledDischarges.contains(audit.getDischargeTime())) {
                // skip cancelled discharges
                continue;
            }
            previousDischarge = audit;
            break;
        }
        return Optional.ofNullable(previousDischarge);
    }

    List<LocationVisitAudit> findAllByLocationVisitIdOrderByStoredUntilDesc(Long locationVisitId);

}
