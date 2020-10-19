package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.movement.AuditLocationVisit;
import uk.ac.ucl.rits.inform.informdb.movement.LocationVisit;

import java.util.List;

/**
 * Audit hospital visit repository.
 * @author Stef Piatek
 */
public interface AuditLocationVisitRepository extends CrudRepository<AuditLocationVisit, Integer> {
    /**
     * @param visit Hospital Visit
     * @return the AuditHospitalVisit
     */
    List<LocationVisit> findAllByHospitalVisitId(HospitalVisit visit);
}
