package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * PatientCondition repository.
 * @author Stef Piatek
 */
public interface PatientConditionRepository extends CrudRepository<PatientCondition, Long> {
    /**
     * Get patient condition, should be unique.
     * @param mrn           MRN entity
     * @param conditionType type of condition
     * @param addedDateTime added date time
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdAndConditionTypeIdAndAddedDateTime(
            Mrn mrn, ConditionType conditionType, Instant addedDateTime
    );

    /**
     * Delete all that is valid before an instant for types.
     * @param untilDateTime instant to delete all messages before
     * @param conditionType data types to delete
     * @return List of patient conditions
     */
    List<PatientCondition> findAllByValidFromLessThanEqualAndInternalIdIsNullAndConditionTypeIdIn(
            Instant untilDateTime, List<ConditionType> conditionType);


    /**
     * For testing, shortcut without requiring entities to be passed.
     * @param mrn           mrn sting
     * @param conditionName name of the condition
     * @param addedDateTime added date time
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdMrnAndConditionTypeIdNameAndAddedDateTime(
            String mrn, String conditionName, Instant addedDateTime
    );

    Optional<PatientCondition> findByConditionTypeIdAndInternalId(ConditionType conditionType, Long internalId);
}
