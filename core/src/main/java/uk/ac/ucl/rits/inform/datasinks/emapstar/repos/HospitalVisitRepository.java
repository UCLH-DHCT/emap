package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.HospitalVisit;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Hospital visit repository.
 * @author Stef Piatek
 */
public interface HospitalVisitRepository extends CrudRepository<HospitalVisit, Integer> {
    /**
     * @param encounter the encounter string
     * @return the HospitalVisit
     */
    Optional<HospitalVisit> findByEncounter(String encounter);

    /**
     * @param mrnId Get visits by Mrn
     * @return hospital visits
     */
    List<HospitalVisit> findAllByMrnIdAndValidFromIsLessThanEqual(Mrn mrnId, Instant untilDate);


    /**
     * Get visits by the mrn's mrnId, used for testing.
     * @param mrnId MrnId
     * @return hospital visits
     */
    Optional<List<HospitalVisit>> findAllByMrnIdMrnId(Long mrnId);
}
