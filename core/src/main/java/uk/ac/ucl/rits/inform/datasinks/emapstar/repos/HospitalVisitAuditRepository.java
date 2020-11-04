package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisitAudit;

/**
 * Audit hospital visit repository.
 * @author Stef Piatek
 */
public interface HospitalVisitAuditRepository extends CrudRepository<HospitalVisitAudit, Long> {
    /**
     * @param encounter the encounter string
     * @return the AuditHospitalVisit
     */
    HospitalVisitAudit findByEncounter(String encounter);
}
