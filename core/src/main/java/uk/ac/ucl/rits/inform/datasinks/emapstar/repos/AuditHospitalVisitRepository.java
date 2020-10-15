package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.AuditHospitalVisit;

/**
 * Audit hospital visit repository.
 * @author Stef Piatek
 */
public interface AuditHospitalVisitRepository extends CrudRepository<AuditHospitalVisit, Integer> {
    /**
     * @param encounter the encounter string
     * @return the AuditHospitalVisit
     */
    AuditHospitalVisit findByEncounter(String encounter);
}
