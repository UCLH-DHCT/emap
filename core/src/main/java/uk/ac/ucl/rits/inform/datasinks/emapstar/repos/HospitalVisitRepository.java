package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;

/**
 * Hospital visit repository.
 * @author Stef Piatek
 */
public interface HospitalVisitRepository extends CrudRepository<HospitalVisit, Integer> {
    /**
     * @param encounter the encounter string
     * @return the HospitalVisit
     */
    HospitalVisit findByEncounterString(String encounter);
}
