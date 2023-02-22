package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolate;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;

import java.util.Optional;

/**
 * Lab Isolate repository.
 * @author Stef Piatek
 */
public interface LabIsolateRepository extends CrudRepository<LabIsolate, Long> {
    Optional<LabIsolate> findByLabResultIdAndLabInternalId(LabResult labResult, String internalId);

    /**
     * for testing.
     * @param isolateCode isolate code
     * @return potential lab isolate
     */
    Optional<LabIsolate> findByIsolateCode(String isolateCode);
}
