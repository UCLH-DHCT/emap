package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.demographics.CoreDemographic;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.Optional;

/**
 * Repository for interaction with the CoreDemographic table.
 */
public interface CoreDemographicRepository extends CrudRepository<CoreDemographic, Integer> {
    /**
     * Get by Mrn object.
     * @param mrn mrn
     * @return core demographic output
     */
    Optional<CoreDemographic> getByMrnIdEquals(Mrn mrn);

    /**
     * Get by mrnId numeric value.
     * @param mrnId mrn id
     * @return core demographic output
     */
    Optional<CoreDemographic> getByMrnIdMrnIdEquals(Long mrnId);

}
