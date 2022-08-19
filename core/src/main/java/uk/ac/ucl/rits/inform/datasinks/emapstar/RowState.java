package uk.ac.ucl.rits.inform.datasinks.emapstar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;
import uk.ac.ucl.rits.inform.informdb.AuditCore;
import uk.ac.ucl.rits.inform.informdb.TemporalCore;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;
import uk.ac.ucl.rits.inform.interchange.adt.PatientClass;
import uk.ac.ucl.rits.inform.interchange.lab.LabResultStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Track the state of a hibernate entity.
 * All values for the entity should be updated from the assign*IfDifferent method of this class to track the state and
 * automatically update the validFrom and storedFrom fields.
 * @param <T> Hibernate Entity type that has validFrom and storedFrom fields.
 * @param <A> The AuditEntity Type
 */
public class RowState<T extends TemporalCore<T, A>, A extends AuditCore> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private T entity;
    private final T originalEntity;
    private final boolean entityCreated;
    private final Instant newValidFrom;
    private final Instant newStoredFrom;
    private boolean entityUpdated = false;

    /**
     * @param entity          hibernate entity
     * @param newValidFrom    valid from timestamp for the "new" value (which may or may not be an actual change)
     * @param newStoredFrom   stored from timestamp to use if the entity is persisted
     * @param entityCreated   whether the entity has just been created (as opposed to already existing in the database)
     */
    public RowState(T entity, Instant newValidFrom, Instant newStoredFrom, boolean entityCreated) {
        this.entity = entity;
        this.newValidFrom = newValidFrom;
        this.newStoredFrom = newStoredFrom;
        this.entityCreated = entityCreated;
        this.originalEntity = entity.copy();
    }

    /**
     * @return was the entity newly created before being wrapped by this RowState object
     */
    public boolean isEntityCreated() {
        return entityCreated;
    }

    /**
     * @return has the entity been updated by this RowState object
     */
    public boolean isEntityUpdated() {
        return entityUpdated;
    }

    /**
     * @return the hibernate entity.
     */
    public T getEntity() {
        return entity;
    }

    /**
     * True if entity has been created, or the message updated time is >= entity validFrom.
     * @param lastUpdatedInstant time that the message was updated
     * @return true if message should be updated
     */
    public boolean messageShouldBeUpdated(Instant lastUpdatedInstant) {
        return entityCreated || !lastUpdatedInstant.isBefore(entity.getValidFrom());
    }

    /**
     * If new value is different assign from InterchangeValue of PatientClass to a setter taking a string.
     * @param newValue        new value
     * @param currentValue    current value
     * @param setPatientClass setter to use if value should be updated
     */
    public void assignInterchangeValue(InterchangeValue<PatientClass> newValue, String currentValue, Consumer<String> setPatientClass) {
        if (newValue.isUnknown()) {
            return;
        }
        assignIfDifferent(newValue.get().toString(), currentValue, setPatientClass);
    }

    /**
     * If new value is different assign from LabResultStatus to a setter taking a string.
     * @param newValue        new value
     * @param currentValue    current value
     * @param setResultStatus setter to use if value should be updated
     */
    public void assignIfDifferent(LabResultStatus newValue, String currentValue, Consumer<String> setResultStatus) {
        assignIfDifferent(newValue.toString(), currentValue, setResultStatus);
    }

    /**
     * Assign new Instant value to LocalDate if different.
     * @param newValue        new value
     * @param currentValue    current value
     * @param setter          setter to use if value should be updated
     */
    public void assignInterchangeValue(InterchangeValue<Instant> newValue, LocalDate currentValue, Consumer<LocalDate> setter) {
        if (newValue.isUnknown()) {
            return;
        }
        Instant unpackedValue = newValue.get();
        LocalDate dateTime = (unpackedValue == null) ? null : unpackedValue.atZone(ZoneId.systemDefault()).toLocalDate();
        assignIfDifferent(dateTime, currentValue, setter);
    }

    /**
     * Assign new byte array value if different.
     * Adds in arrays equal check before assigning if different.
     * @param newValue     new value
     * @param currentValue current value
     * @param setter       setter to use if value should be updated
     */
    public void assignInterchangeValue(InterchangeValue<byte[]> newValue, byte[] currentValue, Consumer<byte[]> setter) {
        if (newValue.isUnknown() || Arrays.equals(newValue.get(), currentValue)) {
            return;
        }
        assignIfDifferent(newValue.get(), currentValue, setter);
    }

    /**
     * If new value is different assign from InterchangeValue to a setter taking the same type.
     * @param newValue     new value
     * @param currentValue current value
     * @param setter       setter to use if value should be updated
     * @param <R>          type of the value in the hibernate entity
     */
    public <R> void assignInterchangeValue(InterchangeValue<R> newValue, R currentValue, Consumer<R> setter) {
        if (newValue.isUnknown()) {
            return;
        }
        assignIfDifferent(newValue.get(), currentValue, setter);
    }

    /**
     * Directly assign a new value if it is different from current value.
     * @param newValue     new value
     * @param currentValue current value
     * @param setter       setter to use if value should be updated
     * @param <R>          type of the value in the hibernate entity
     * @return true if state has been updated
     */
    public <R> boolean assignIfDifferent(@Nullable R newValue, @Nullable R currentValue, Consumer<R> setter) {
        if (Objects.equals(newValue, currentValue)) {
            return false;
        }
        entityUpdated = true;
        setter.accept(newValue);
        return true;
    }


    /**
     * If current value exists, remove it and set validFrom cancellation time.
     * @param currentValue      current value
     * @param setter            setter to use if value should be updated
     * @param cancelledDateTime Time of cancellation
     * @param <R>               type of the value in the hibernate entity
     */
    public <R> void removeIfExists(@Nullable R currentValue, Consumer<R> setter, Instant cancelledDateTime) {
        boolean removed = assignIfDifferent(null, currentValue, setter);
        if (removed && cancelledDateTime != null) {
            entity.setValidFrom(cancelledDateTime);
        }
    }

    /**
     * Save entity if it is created, or auditlog if the entity has been updated, setting stored from and valid from if either conditions are true.
     * <p>
     * Entity needs to be manually saved in order to persist an entity from cache where it is either returned or an exception is thrown.
     * @param entityRepo entity repository
     * @param auditRepo  audit repository
     */
    public void saveEntityOrAuditLogIfRequired(CrudRepository<T, Long> entityRepo, CrudRepository<A, Long> auditRepo) {
        if (entityCreated) {
            entity.setStoredFrom(newStoredFrom);
            entity.setValidFrom(newValidFrom);
            logger.debug("New Entity saved: {}", entityRepo.save(entity));
        } else if (entityUpdated) {
            entity.setStoredFrom(newStoredFrom);
            entity.setValidFrom(newValidFrom);
            entityRepo.save(entity);
            A auditEntity = originalEntity.createAuditEntity(newValidFrom, newStoredFrom);
            auditRepo.save(auditEntity);
            logger.debug("New AuditEntity being saved: {}", auditEntity);
        }
    }

    /**
     * Convenience method to allow assignment of fields which should always be added to if currently null in database.
     * If a value exists, then should only update the value if the message is newer.
     * @param msgValue         interchange value from message
     * @param currentValue     current value
     * @param setter           setter to use if value should be updated
     * @param messageValidFrom updateTime of the message
     * @param entityValidFrom  validFrom from the database entity
     * @param <R>              type of the value in the hibernate entity
     */
    public <R> void assignIfCurrentlyNullOrNewerAndDifferent(
            InterchangeValue<R> msgValue, R currentValue, Consumer<R> setter, Instant messageValidFrom, Instant entityValidFrom
    ) {
        if (currentValue == null || messageValidFrom.isAfter(entityValidFrom)) {
            assignInterchangeValue(msgValue, currentValue, setter);
        }
    }

    /**
     * Convenience method to allow assignment of fields which should always be added to if currently null in database.
     * If a value exists, then should only update the value if the message is newer.
     * @param msgValue         value from message
     * @param currentValue     current value
     * @param setter           setter to use if value should be updated
     * @param messageValidFrom updateTime of the message
     * @param entityValidFrom  validFrom from the database entity
     * @param <R>              type of the value in the hibernate entity
     */
    public <R> void assignIfCurrentlyNullOrNewerAndDifferent(
            R msgValue, R currentValue, Consumer<R> setter, Instant messageValidFrom, Instant entityValidFrom) {
        if (currentValue == null || messageValidFrom.isAfter(entityValidFrom)) {
            assignIfDifferent(msgValue, currentValue, setter);
        }
    }
}
