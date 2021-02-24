package uk.ac.ucl.rits.inform.datasinks.emapstar.repos.labs;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.datasinks.emapstar.RowState;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolate;
import uk.ac.ucl.rits.inform.informdb.labs.LabIsolateAudit;
import uk.ac.ucl.rits.inform.informdb.labs.LabResult;
import uk.ac.ucl.rits.inform.informdb.labs.LabSensitivity;

import java.util.Optional;

/**
 * Lab Isolate repository.
 * @author Stef Piatek
 */
public interface LabIsolateRepository extends CrudRepository<LabIsolate, Long> {
    Optional<LabIsolate> findByLabResultIdAndIsolateCode(LabResult labResult, String isolateCode);

    /**
     * for testing
     * @param isolateCode isolate code
     * @return potential lab isolate
     */
    Optional<LabIsolate> findByIsolateCode(String isolateCode);
}
