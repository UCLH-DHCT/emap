package uk.ac.ucl.rits.inform.datasinks.emapstar.repos;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ucl.rits.inform.informdb.conditions.ConditionType;
import uk.ac.ucl.rits.inform.informdb.conditions.PatientCondition;
import uk.ac.ucl.rits.inform.informdb.identity.Mrn;

import java.time.Instant;
import java.time.LocalDate;
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
     * @param addedDatetime added date time
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdAndConditionTypeIdAndAddedDatetime(
            Mrn mrn, ConditionType conditionType, Instant addedDatetime
    );

    /**
     * Get patient condition, should be unique.
     * @param mrn           MRN entity
     * @param conditionType type of condition
     * @param onsetDate     onset date of condition
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdAndConditionTypeIdAndOnsetDate(
            Mrn mrn, ConditionType conditionType, LocalDate onsetDate
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
     * @param internalCode  EPIC code of the condition
     * @param addedDatetime added date time
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdMrnAndConditionTypeIdInternalCodeAndAddedDatetime(
            String mrn, String internalCode, Instant addedDatetime
    );

    /**
     * For testing, shortcut without requiring entities to be passed.
     * @param mrn           mrn sting
     * @param internalCode  EPIC code of the condition
     * @param onsetDate     onset date of condition
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdMrnAndConditionTypeIdInternalCodeAndOnsetDate(
            String mrn, String internalCode, LocalDate onsetDate
    );

    Optional<PatientCondition> findByMrnIdMrn(String mrn);

    Optional<PatientCondition> findByConditionTypeIdAndInternalId(ConditionType conditionType, Long internalId);
}
