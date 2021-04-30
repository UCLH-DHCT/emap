package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;
import uk.ac.ucl.rits.inform.informdb.state.PatientState;
import uk.ac.ucl.rits.inform.informdb.state.PatientStateType;

import java.time.Instant;
import java.util.Optional;

/**
 * PatientState repository.
 * @author Stef Piatek
 */
public interface PatientStateRepository extends CrudRepository<PatientState, Long> {
    /**
     * Get patient state, should be unique.
     * @param mrn              MRN entity
     * @param patientStateType type of state
     * @param addedDateTime    added date time
     * @return possible patient state
     */
    Optional<PatientState> findByMrnIdAndPatientStateTypeIdAndAddedDateTime(
            Mrn mrn, PatientStateType patientStateType, Instant addedDateTime
    );


    /**
     * For testing, shortcut without requiring entities to be passed.
     * @param mrn           mrn sting
     * @param stateName     name of the state
     * @param addedDateTime added date time
     * @return possible patient state
     */
    Optional<PatientState> findByMrnIdMrnAndPatientStateTypeIdNameAndAddedDateTime(
            String mrn, String stateName, Instant addedDateTime
    );
}
