package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.util.Optional;

public interface MrnRepository extends CrudRepository<Mrn, Integer> {
    Optional<Mrn> getByMrnEqualsOrMrnIsNullAndNhsNumberEquals(String mrn, String nhsNumber);
}
