package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.time.Instant;
import java.util.Optional;

/**
 * PatientCondition repository.
 * @author Stef Piatek
 */
public interface PatientConditionRepository extends CrudRepository<PatientCondition, Long> {
    /**
     * Get patient state, should be unique.
     * @param mrn           MRN entity
     * @param conditionType type of state
     * @param addedDateTime added date time
     * @return possible patient state
     */
    Optional<PatientCondition> findByMrnIdAndConditionTypeIdAndAddedDateTime(
            Mrn mrn, ConditionType conditionType, Instant addedDateTime
    );

    /**
     * Delete all that is valid before an instant for types.
     * @param untilDateTime instant to delete all messages before
     * @param conditionType      data type
     */
    void deleteAllByValidFromBeforeAndInternalIdIsNullAndConditionTypeId(Instant untilDateTime, ConditionType conditionType);


    /**
     * For testing, shortcut without requiring entities to be passed.
     * @param mrn           mrn sting
     * @param stateName     name of the state
     * @param addedDateTime added date time
     * @return possible patient state
     */
    Optional<PatientCondition> findByMrnIdMrnAndConditionTypeIdNameAndAddedDateTime(
            String mrn, String stateName, Instant addedDateTime
    );

    Optional<PatientCondition> findByConditionTypeIdAndInternalId(ConditionType conditionType, Long internalId);
}
