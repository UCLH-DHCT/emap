package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.movement.AuditLocationVisit;

import java.time.Instant;
import java.util.Optional;

/**
 * Audit hospital visit repository.
 * @author Stef Piatek
 */
public interface AuditLocationVisitRepository extends CrudRepository<AuditLocationVisit, Integer> {
    /**
     * For testing: find by location string.
     * @param location full location string
     * @return AuditLocationVisit wrapped in optional
     */
    Optional<AuditLocationVisit> findByLocationIdLocationString(String location);

    /**
     * For testing: find by location string.
     * @param location  full location string
     * @param validFrom valid from Instant
     * @return AuditLocationVisit wrapped in optional
     */
    Optional<AuditLocationVisit> findByLocationIdLocationStringAndValidFrom(String location, Instant validFrom);

}
