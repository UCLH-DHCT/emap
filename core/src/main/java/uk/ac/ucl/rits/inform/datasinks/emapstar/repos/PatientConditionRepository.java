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
 *
 * @author Stef Piatek
 */
public interface PatientConditionRepository extends CrudRepository<PatientCondition, Long> {
    /**
     * Get patient condition, should be unique.
     *
     * @param mrn           MRN entity
     * @param conditionType type of condition
     * @param internalId    identifier used for condition in EPIC
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdAndConditionTypeIdAndInternalId(
            Mrn mrn, ConditionType conditionType, Long internalId
    );

    /**
     * Get patient condition, should be unique.
     *
     * @param mrn           MRN entity
     * @param conditionType type of condition
     * @param addedDatetime date and time when patient condition was added
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdAndConditionTypeIdAndAddedDatetime(
            Mrn mrn, ConditionType conditionType, Instant addedDatetime
    );

    /**
     * Get patient condition, should be unique.
     *
     * @param mrn           MRN entity
     * @param conditionType type of condition
     * @param addedDate     date and time when patient condition was added
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdAndConditionTypeIdAndAddedDate(
            Mrn mrn, ConditionType conditionType, LocalDate addedDate
    );

    /**
     * Delete all that is valid before an instant for types.
     *
     * @param untilDateTime instant to delete all messages before
     * @param conditionType data types to delete
     * @return List of patient conditions
     */
    List<PatientCondition> findAllByValidFromLessThanEqualAndInternalIdIsNullAndConditionTypeIdIn(
            Instant untilDateTime, List<ConditionType> conditionType);


    /**
     * For testing, shortcut without requiring entities to be passed.
     *
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
     *
     * @param mrn          mrn sting
     * @param internalCode EPIC code of the condition
     * @param onsetDate    onset date of condition
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdMrnAndConditionTypeIdInternalCodeAndOnsetDate(
            String mrn, String internalCode, LocalDate onsetDate
    );

    Optional<PatientCondition> findByMrnIdMrn(String mrn);

    Optional<PatientCondition> findByConditionTypeIdAndInternalId(ConditionType conditionType, Long internalId);


    /**
     * For testing only.
     * @param mrn mrn string
     * @param comment comment string
     * @return possible patient condition
     */
    Optional<PatientCondition> findByMrnIdMrnAndComment(String mrn, String comment);
}
